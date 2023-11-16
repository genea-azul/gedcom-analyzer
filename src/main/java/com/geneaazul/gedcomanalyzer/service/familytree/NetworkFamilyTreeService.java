package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.PyvisNetworkMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.PythonUtils;

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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkFamilyTreeService implements FamilyTreeService {

    private final GedcomHolder gedcomHolder;
    private final FamilyTreeHelper familyTreeHelper;
    private final PyvisNetworkMapper pyvisNetworkMapper;
    private final GedcomAnalyzerProperties properties;

    public Path getNetworkHtmlFile(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix) {

        return properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(familyTreeFileIdPrefix + "_" + person.getUuid() + familyTreeFileSuffix + ".html");
    }

    @Override
    public boolean isMissingFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix,
            boolean obfuscateLiving) {

        Path htmlPyvisNetworkFilePath = getNetworkHtmlFile(
                person,
                familyTreeFileIdPrefix,
                familyTreeFileSuffix);

        return Files.notExists(htmlPyvisNetworkFilePath);
    }

    @Override
    public void generateFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix,
            boolean obfuscateLiving,
            List<List<Relationship>> relationshipsWithNotInLawPriority) {

        Path htmlPyvisNetworkFilePath = getNetworkHtmlFile(
                person,
                familyTreeFileIdPrefix,
                familyTreeFileSuffix);

        Path csvPyvisNetworkNodesFilePath = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(familyTreeFileIdPrefix + "_nodes_" + person.getUuid() + familyTreeFileSuffix + ".csv");

        Path csvPyvisNetworkEdgesFilePath = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(familyTreeFileIdPrefix + "_edges_" + person.getUuid() + familyTreeFileSuffix + ".csv");

        List<EnrichedPerson> peopleToExport = relationshipsWithNotInLawPriority
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

    public Optional<Path> getFamilyTree(
            UUID personUuid,
            boolean obfuscateLiving) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = gedcom.getPersonByUuid(personUuid);
        if (person == null) {
            return Optional.empty();
        }

        String familyTreeFileIdPrefix = familyTreeHelper.getFamilyTreeFileId(person);
        String familyTreeFileSuffix = obfuscateLiving ? "" : "_visible";

        Path htmlPyvisNetworkFilePath = getNetworkHtmlFile(
                person,
                familyTreeFileIdPrefix,
                familyTreeFileSuffix);

        if (Files.notExists(htmlPyvisNetworkFilePath)) {
            List<List<Relationship>> relationshipsWithNotInLawPriority = familyTreeHelper
                    .getRelationshipsWithNotInLawPriority(person);

            generateFamilyTree(
                    person,
                    familyTreeFileIdPrefix,
                    familyTreeFileSuffix,
                    obfuscateLiving,
                    relationshipsWithNotInLawPriority);
        }

        return Optional.of(htmlPyvisNetworkFilePath);
    }

    public void generateNetworkHTML(
            Path htmlPyvisNetworkFilePath,
            Path csvPyvisNetworkNodesFilePath,
            Path csvPyvisNetworkEdgesFilePath,
            boolean obfuscateLiving,
            List<EnrichedPerson> peopleInTree) throws IOException {
        log.info("Generating Network family tree HTML");

        // Make sure target directory exists
        Files.createDirectories(htmlPyvisNetworkFilePath.getParent());

        // Generate files
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

        String defaultSpouseTitle = "pareja de";
        String separatedTitle = "ex-pareja";
        String defaultChildTitle = "hijo";
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
                                        defaultSpouseTitle,
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
                                                    swc.getSpouse().isEmpty(),
                                                    cwr.referenceType().isPresent(),
                                                    defaultChildTitle,
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

        String python3Command = PythonUtils.getPython3Command();

        Path scriptPath = properties
                .getTempDir()
                .resolve(properties.getPyvisNetworkExportScriptFilename())
                .toAbsolutePath()
                .normalize();

        CommandLine commandLine = CommandLine
                .parse(python3Command + " " + scriptPath)
                .addArgument(htmlPyvisNetworkFilePath.getParent().toAbsolutePath().normalize().toString(), true)
                .addArgument(htmlPyvisNetworkFilePath.getFileName().toString(), true)
                .addArgument(csvPyvisNetworkNodesFilePath.getFileName().toString(), true)
                .addArgument(csvPyvisNetworkEdgesFilePath.getFileName().toString(), true);

        DefaultExecutor executor = new DefaultExecutor();
        executor.execute(commandLine);

        if (Files.notExists(htmlPyvisNetworkFilePath)) {
            log.error("File is missing [ path={} ]", htmlPyvisNetworkFilePath);
            return;
        }

        Charset charset = StandardCharsets.UTF_8;
        String content = Files.readString(htmlPyvisNetworkFilePath, charset);

        content = content.replace(
                "<script src=\"lib/bindings/utils.js\"></script>",
                "<script src=\"/js/family-tree/utils.js\"></script>");

        content = content.replace(
                "<h1></h1>",
                "");
        // Twice same sentence
        content = content.replace(
                "<h1></h1>",
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

        content = content.replace(
                "font-size:22px;",
                "display: none;");
        content = content.replace(
                "width:500px;",
                "width: 95%;");
        content = content.replace(
                "top:400px;",
                "top: 40%;");
        content = content.replace(
                "width:600px;",
                "width: 50%;");

        Files.writeString(htmlPyvisNetworkFilePath, content, charset);
    }

}
