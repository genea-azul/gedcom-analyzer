package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.PyvisNetworkMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.PersonService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NetworkFamilyTreeService extends FamilyTreeService {

    private final GedcomHolder gedcomHolder;
    private final PyvisNetworkMapper pyvisNetworkMapper;
    private final GedcomAnalyzerProperties properties;

    public NetworkFamilyTreeService(
            PersonService personService,
            GedcomHolder gedcomHolder,
            PyvisNetworkMapper pyvisNetworkMapper,
            GedcomAnalyzerProperties properties) {
        super(personService);
        this.gedcomHolder = gedcomHolder;
        this.pyvisNetworkMapper = pyvisNetworkMapper;
        this.properties = properties;
    }

    public Path getNetworkHtmlFile(
            UUID personUuid,
            boolean obfuscateLiving) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = gedcom.getPersonByUuid(personUuid);

        if (person == null) {
            return null;
        }

        return getNetworkHtmlFile(person, obfuscateLiving);
    }

    public Path getNetworkHtmlFile(
            EnrichedPerson person,
            boolean obfuscateLiving) {

        String fileId = getFamilyTreeFileId(person);
        String suffix = obfuscateLiving ? "" : "_visible";

        return properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(fileId + "_" + person.getUuid() + suffix + ".html");
    }

    @Override
    public void generateFamilyTree(
            EnrichedPerson person,
            boolean obfuscateLiving) {

        String fileId = getFamilyTreeFileId(person);
        String suffix = obfuscateLiving ? "" : "_visible";

        Path htmlPyvisNetworkFilePath = getNetworkHtmlFile(person, obfuscateLiving);

        if (Files.exists(htmlPyvisNetworkFilePath)) {
            return;
        }

        Path csvPyvisNetworkNodesFilePath = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(fileId + "_nodes_" + person.getUuid() + suffix + ".csv");

        Path csvPyvisNetworkEdgesFilePath = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(fileId + "_edges_" + person.getUuid() + suffix + ".csv");

        List<EnrichedPerson> peopleToExport = getRelationshipsWithNotInLawPriority(person)
                .stream()
                .limit(properties.getMaxPyvisNetworkNodesToExport())
                .map(relationships -> relationships.get(0))
                .map(Relationship::person)
                .toList();

        try {
            generateNetworkHTML(
                    htmlPyvisNetworkFilePath,
                    csvPyvisNetworkNodesFilePath,
                    csvPyvisNetworkEdgesFilePath,
                    obfuscateLiving,
                    peopleToExport);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void generateNetworkHTML(
            Path htmlPyvisNetworkFilePath,
            Path csvPyvisNetworkNodesFilePath,
            Path csvPyvisNetworkEdgesFilePath,
            boolean obfuscateLiving,
            List<EnrichedPerson> peopleInTree) throws IOException {
        exportToPyvisNodesCSV(csvPyvisNetworkNodesFilePath, peopleInTree, obfuscateLiving);
        exportToPyvisEdgesCSV(csvPyvisNetworkEdgesFilePath, peopleInTree);
        exportToPyvisNetworkHTML(htmlPyvisNetworkFilePath, csvPyvisNetworkNodesFilePath, csvPyvisNetworkEdgesFilePath);
    }

    public void exportToPyvisNodesCSV(Path path, List<EnrichedPerson> people, boolean obfuscateLiving) throws IOException {

        String[] HEADERS = {"id", "label", "title", "shape", "borderWidth", "color", "size"};

        Map<String, String[]> countryColorsMap = Map.of(
                "Argentina", new String[] {"#9AE4FF", "#74ACDF"},
                "Italia", new String[] {"#00B65A", "#008C45"},
                "Francia", new String[] {"#0071DA", "#0055A4"},
                "España", new String[] {"#E61C21", "#AD1519"},
                "Inglaterra", new String[] {"#E61C21", "#AD1519"},
                "Irlanda", new String[] {"#E61C21", "#AD1519"},
                "Países Bajos", new String[] {"#E61C21", "#AD1519"});

        String defaultLabel = "?";
        double defaultNodeBorderWidth = 2;
        String[] defaultColors = new String[] {"#000000", "#666666"};
        double defaultSize = 25;

        double coupleNodeBorderWidth = 1;
        String coupleNodeColor = "#000000";
        double coupleNodeSize = 7.5;

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .build();

        Set<String> idsToExport = people
                .stream()
                .map(EnrichedPerson::getId)
                .collect(Collectors.toUnmodifiableSet());

        try (FileWriter fw = new FileWriter(path.toFile(), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(fw, csvFormat)) {

            people.forEach(person -> {
                try {
                    String[] scvRecord = pyvisNetworkMapper.toPyvisNodeCsvRecord(
                            person,
                            obfuscateLiving,
                            countryColorsMap,
                            defaultLabel,
                            defaultNodeBorderWidth,
                            defaultColors,
                            defaultSize);
                    printer.printRecord((Object[]) scvRecord);

                    person
                            .getSpousesWithChildren()
                            .stream()
                            .filter(swc -> swc.getSpouse().isPresent())
                            .filter(swc -> idsToExport.contains(swc.getSpouse().get().getId()))
                            .filter(swc -> person.getOrderKey().compareTo(swc.getSpouse().get().getOrderKey()) < 0)
                            .filter(swc -> hasChildrenToExport(swc.getChildren(), idsToExport))
                            .forEach(swc -> {
                                try {
                                    String[] coupleCsvRecord = pyvisNetworkMapper.toPyvisCoupleNodeCsvRecord(
                                            person,
                                            swc.getSpouse().get(),
                                            false,
                                            obfuscateLiving,
                                            defaultLabel,
                                            coupleNodeBorderWidth,
                                            coupleNodeColor,
                                            coupleNodeSize);
                                    printer.printRecord((Object[]) coupleCsvRecord);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public void exportToPyvisEdgesCSV(Path path, List<EnrichedPerson> people) throws IOException {

        String[] HEADERS = {"source", "target", "title", "weight", "width"};

        String separatedTitle = "ex-pareja";
        String adoptedTitle = "adoptivo";
        double coupleWeight = 2.5;
        double coupleWidth = 5;
        double defaultWeight = 1;
        double defaultWidth = 1.5;

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .build();

        Set<String> idsToExport = people
                .stream()
                .map(EnrichedPerson::getId)
                .collect(Collectors.toUnmodifiableSet());

        try (FileWriter fw = new FileWriter(path.toFile(), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(fw, csvFormat)) {

            Set<String> processedChildren = new HashSet<>();

            people.forEach(person -> {
                person
                        .getSpousesWithChildren()
                        .stream()
                        .filter(swc -> swc.getSpouse().isPresent())
                        .filter(swc -> idsToExport.contains(swc.getSpouse().get().getId()))
                        .filter(swc -> hasChildrenToExport(swc.getChildren(), idsToExport)
                                || person.getOrderKey().compareTo(swc.getSpouse().get().getOrderKey()) < 0)
                        .forEach(swc -> {
                            try {
                                String[] coupleCsvRecord = pyvisNetworkMapper.toPyvisSpouseEdgeCsvRecord(
                                        person,
                                        swc.getSpouse().get(),
                                        !hasChildrenToExport(swc.getChildren(), idsToExport),
                                        swc.isSeparated(),
                                        separatedTitle,
                                        coupleWeight,
                                        coupleWidth);
                                printer.printRecord((Object[]) coupleCsvRecord);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });

                person
                        .getSpousesWithChildren()
                        .stream()
                        .filter(swc -> swc.getSpouse().isEmpty() || idsToExport.contains(swc.getSpouse().get().getId()))
                        .filter(swc -> hasChildrenToExport(swc.getChildren(), idsToExport))
                        .forEach(swc -> {
                            String sourceId = swc.getSpouse().isEmpty()
                                    ? person.getId()
                                    : pyvisNetworkMapper.buildCoupleNodeId(person, swc.getSpouse().get(), false);
                            swc.getChildrenWithReference()
                                    .stream()
                                    .filter(cwr -> idsToExport.contains(cwr.person().getId()))
                                    .filter(cwr -> !processedChildren.contains(sourceId + "|" + cwr.person().getId()))
                                    .forEach(cwr -> {
                                        try {
                                            String[] childCsvRecord = pyvisNetworkMapper.toPyvisChildEdgeCsvRecord(
                                                    sourceId,
                                                    cwr.person().getId(),
                                                    cwr.referenceType().isPresent(),
                                                    adoptedTitle,
                                                    defaultWeight,
                                                    defaultWidth);
                                            printer.printRecord((Object[]) childCsvRecord);
                                            processedChildren.add(sourceId + "|" + cwr.person().getId());
                                        } catch (IOException e) {
                                            throw new UncheckedIOException(e);
                                        }
                                    });
                        });
            });
        }
    }

    private boolean hasChildrenToExport(List<EnrichedPerson> children, Set<String> idsToExport) {
        return !children.isEmpty()
                && children
                .stream()
                .anyMatch(child -> idsToExport.contains(child.getId()));
    }

    private void exportToPyvisNetworkHTML(
            Path htmlPyvisNetworkFilePath,
            Path csvPyvisNetworkNodesFilePath,
            Path csvPyvisNetworkEdgesFilePath) throws IOException {

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(properties.getPyvisNetworkExportScriptPath());

        CommandLine commandLine = CommandLine
                .parse("python3 " + resource.getURI().getPath())
                .addArgument(htmlPyvisNetworkFilePath.getParent().toAbsolutePath().normalize().toString(), true)
                .addArgument(htmlPyvisNetworkFilePath.getFileName().toString(), true)
                .addArgument(csvPyvisNetworkNodesFilePath.getFileName().toString(), true)
                .addArgument(csvPyvisNetworkEdgesFilePath.getFileName().toString(), true);

        DefaultExecutor executor = new DefaultExecutor();
        executor.execute(commandLine);

        if (Files.exists(htmlPyvisNetworkFilePath)) {
            Charset charset = StandardCharsets.UTF_8;
            String content = Files.readString(htmlPyvisNetworkFilePath, charset);
            content = content.replace(
                    "<script src=\"lib/bindings/utils.js\"></script>",
                    "<script src=\"/js/family-tree/utils.js\"></script>");
            content = content.replace(
                    "<center>\n"
                            + "<h1></h1>\n"
                            + "</center>",
                    "");
            content = content.replace(
                    "        <center>\n"
                            + "          <h1></h1>\n"
                            + "        </center>",
                    "");
            content = content.replace(
                    "height: 600px;",
                    "height: 100%;");
            // Twice same sentence
            content = content.replace(
                    "height: 600px;",
                    "height: 100%;");
            content = content.replace(
                    "border: 1px solid lightgray;",
                    "border: 0px; padding: 0px;");
            content = content.replace(
                    "<div class=\"card\" style=\"width: 100%\">",
                    "<div class=\"card\" style=\"width: 100%; height: 100%; border: 0px;\">");
            Files.writeString(htmlPyvisNetworkFilePath, content, charset);
        }
    }

}
