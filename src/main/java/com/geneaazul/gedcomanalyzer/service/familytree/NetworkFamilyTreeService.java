package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.PyvisNetworkMapper;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.PythonUtils;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private final RelationshipMapper relationshipMapper;
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
            String familyTreeFileSuffix) {

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
            boolean onlySecondaryDescription,
            List<List<Relationship>> relationshipsWithNotInLawPriority) {

        log.info("Generating network family tree HTML [ personId={}, personUuid={} ]", person.getId(), person.getUuid());
        long startTime = System.currentTimeMillis();

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

        generate(
                htmlPyvisNetworkFilePath,
                csvPyvisNetworkNodesFilePath,
                csvPyvisNetworkEdgesFilePath,
                obfuscateLiving,
                relationshipsWithNotInLawPriority);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Completed network family tree HTML [ personId={}, personUuid={}, ms={} ]", person.getId(), person.getUuid(), totalTime);
    }

    @Override
    public Optional<FamilyTree> getFamilyTree(
            UUID personUuid,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            boolean forceRewrite) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = gedcom.getPersonByUuid(personUuid);
        if (person == null) {
            log.warn("Person not found [ personUuid={}, modifiedDateTime={} ]", personUuid, gedcom.getModifiedDateTime());
            return Optional.empty();
        }

        String familyTreeFileIdPrefix = familyTreeHelper.getFamilyTreeFileId(person);
        String familyTreeFileSuffix = obfuscateLiving ? "" : "_visible";

        Path htmlPyvisNetworkFilePath = getNetworkHtmlFile(
                person,
                familyTreeFileIdPrefix,
                familyTreeFileSuffix);

        if (forceRewrite || Files.notExists(htmlPyvisNetworkFilePath)) {
            List<List<Relationship>> relationshipsWithNotInLawPriority = familyTreeHelper
                    .getRelationshipsWithNotInLawPriority(person);

            generateFamilyTree(
                    person,
                    familyTreeFileIdPrefix,
                    familyTreeFileSuffix,
                    obfuscateLiving,
                    onlySecondaryDescription,
                    relationshipsWithNotInLawPriority);
        }

        return Optional.of(new FamilyTree(
                FamilyTreeType.NETWORK,
                person,
                "genea_azul_arbol_" + familyTreeFileIdPrefix + ".html",
                htmlPyvisNetworkFilePath,
                MediaType.TEXT_HTML,
                properties.getLocale()));
    }

    @VisibleForTesting
    protected void generate(
            Path htmlPyvisNetworkFilePath,
            Path csvPyvisNetworkNodesFilePath,
            Path csvPyvisNetworkEdgesFilePath,
            boolean obfuscateLiving,
            List<List<Relationship>> peopleInTree) {

        List<Relationship> peopleToExport = peopleInTree
                .stream()
                .limit(properties.getMaxPyvisNetworkNodesToExport())
                .map(List::getFirst)
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

    private void generateNetworkHTML(
            Path htmlPyvisNetworkFilePath,
            Path csvPyvisNetworkNodesFilePath,
            Path csvPyvisNetworkEdgesFilePath,
            boolean obfuscateLiving,
            List<Relationship> relationships) throws IOException {

        // Make sure target directory exists
        Files.createDirectories(htmlPyvisNetworkFilePath.getParent());

        List<EnrichedPerson> peopleInTree = relationships.stream()
                .map(Relationship::person)
                .toList();

        Map<String, String[]> countryColorsMap = getCountryColorsMap();

        List<Pair<String, String>> countryToColorForLegend = peopleInTree.stream()
                .flatMap(p -> p.getPlaceOfBirth().stream())
                .map(Place::country)
                .distinct()
                .sorted()
                .map(c -> Pair.of(
                        c,
                        Optional.ofNullable(countryColorsMap.get(c))
                                .map(colors -> colors[0])
                                .filter(StringUtils::isNotEmpty)
                                .orElse("#666666")))
                .toList();

        Map<Integer, FormattedRelationship> relationshipToRoot = relationships.stream()
                .collect(Collectors.toMap(
                        r -> r.person().getId(),
                        r -> relationshipMapper.formatInSpanish(
                                relationshipMapper.toRelationshipDto(r, false), false)));

        // Generate files
        EnrichedPerson rootPerson = relationships.getFirst().person();
        exportToPyvisNodesCSV(csvPyvisNetworkNodesFilePath, peopleInTree, obfuscateLiving, rootPerson, countryColorsMap, relationshipToRoot);
        exportToPyvisEdgesCSV(csvPyvisNetworkEdgesFilePath, peopleInTree);
        exportToPyvisNetworkHTML(htmlPyvisNetworkFilePath, csvPyvisNetworkNodesFilePath, csvPyvisNetworkEdgesFilePath, rootPerson.getDisplayName(), countryToColorForLegend);
    }

    private static Map<String, String[]> getCountryColorsMap() {
        return Map.ofEntries(
                Map.entry("Alemania", new String[] {"#FFF40F", "#FFCC00"}),
                Map.entry("Argentina", new String[] {"#8BCEFF", "#74ACDF"}),
                Map.entry("Australia", new String[] {"#12AE54", "#0F9146"}),
                Map.entry("Austria", new String[] {"", ""}),
                Map.entry("Bolivia", new String[] {"#0F923D", "#F4E400"}),
                Map.entry("Brasil", new String[] {"#0F2E8D", "#FFDF00"}),
                Map.entry("Bulgaria", new String[] {"", ""}),
                Map.entry("Bélgica", new String[] {"#FFFF2B", "#FDDA24"}),
                Map.entry("Checoslovaquia", new String[] {"#145297", "#11457E"}),
                Map.entry("Chile", new String[] {"#0F44C7", "#0039A6"}),
                Map.entry("China", new String[] {"", ""}),
                Map.entry("Croacia", new String[] {"#1B1BB4", "#171796"}),
                Map.entry("Dinamarca", new String[] {"#F01337", "#C8102E"}),
                Map.entry("Ecuador", new String[] {"", ""}),
                Map.entry("Escocia", new String[] {"#0F70DC", "#005EB8"}),
                Map.entry("España", new String[] {"#CF191E", "#AD1519"}),
                Map.entry("Estados Unidos", new String[] {"#484684", "#3C3B6E"}),
                Map.entry("Francia", new String[] {"#0F66C4", "#0055A4"}),
                Map.entry("Guatemala", new String[] {"", ""}),
                Map.entry("Hungría", new String[] {"", ""}),
                Map.entry("Inglaterra", new String[] {"#F7142B", "#CE1124"}),
                Map.entry("Irlanda", new String[] {"#FFA34A", "#FF883E"}),
                Map.entry("Irlanda del Norte", new String[] {"", ""}),
                Map.entry("Italia", new String[] {"#0FA852", "#008C45"}),
                Map.entry("Jamaica", new String[] {"", ""}),
                Map.entry("Japón", new String[] {"#E10F36", "#BC002D"}),
                Map.entry("Líbano", new String[] {"#0FC860", "#00A750"}),
                Map.entry("Marruecos", new String[] {"#0F753D", "#006233"}),
                Map.entry("Nicaragua", new String[] {"", ""}),
                Map.entry("Océano Atlántico", new String[] {"", ""}),
                Map.entry("Paraguay", new String[] {"#0F43C9", "#0038A8"}),
                Map.entry("Países Bajos", new String[] {"#24559F", "#1E4785"}),
                Map.entry("Perú", new String[] {"#FF132A", "#D91023"}),
                Map.entry("Polonia", new String[] {"#FF1848", "#DC143C"}),
                Map.entry("Portugal", new String[] {"#0F7A0F", "#006600"}),
                Map.entry("República Dominicana", new String[] {"", ""}),
                Map.entry("Rusia", new String[] {"#0F3CC0", "#0032A0"}),
                Map.entry("Siria", new String[] {"#0F9249", "#007A3D"}),
                Map.entry("Sudáfrica", new String[] {"", ""}),
                Map.entry("Suiza", new String[] {"#FF3102", "#DA2902"}),
                Map.entry("Uruguay", new String[] {"#8CCCFF", "#75AADB"}),
                Map.entry("Yugoslavia", new String[] {"#0F43B0", "#003893"}));
    }

    public void exportToPyvisNodesCSV(
            Path path, List<EnrichedPerson> people,
            boolean obfuscateLiving,
            EnrichedPerson rootPerson,
            Map<String, String[]> countryColorsMap,
            Map<Integer, FormattedRelationship> relationshipToRoot) throws IOException {

        String[] HEADERS = {"id", "label", "title", "shape", "borderWidth", "color", "size"};

        String defaultLabel = "?";
        double defaultNodeBorderWidth = 2;
        String[] defaultColors = new String[] {"#666666", "#B7B7B7"};
        double defaultSize = 25;

        double coupleNodeBorderWidth = 1;
        String coupleNodeColor = "#6b7280";
        double coupleNodeSize = 6;

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .get();

        Set<Integer> idsToExport = people
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
                            defaultSize,
                            person.getId().equals(rootPerson.getId()),
                            relationshipToRoot.get(person.getId()));
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

        String[] HEADERS = {"source", "target", "title", "weight", "width", "color", "dashes"};

        String defaultSpouseTitle = "pareja de";
        String separatedTitle = "ex-pareja";
        String defaultChildTitle = "hijo";
        String adoptedTitle = "adoptivo";
        int adoptedDashes = 2;
        double coupleWeight = 2.5;
        double coupleWidth = 2;
        String coupleColor = "#b0b0b0";
        int coupleDashes = 5;
        double defaultWeight = 1;
        double defaultWidth = 1.5;
        String defaultColor = "#6b7280";
        int defaultDashes = 0;

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .get();

        Set<Integer> idsToExport = people
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
                                        coupleWidth,
                                        coupleColor,
                                        coupleDashes);
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
                                    ? person.getId().toString()
                                    : pyvisNetworkMapper.buildCoupleNodeId(person, swc.getSpouse().get(), false);
                            swc.getChildrenWithReference()
                                    .stream()
                                    .filter(cwr -> idsToExport.contains(cwr.person().getId()))
                                    .filter(cwr -> !processedChildren.contains(sourceId + "|" + cwr.person().getId()))
                                    .forEach(cwr -> {
                                        try {
                                            String[] childCsvRecord = pyvisNetworkMapper.toPyvisChildEdgeCsvRecord(
                                                    sourceId,
                                                    cwr.person().getId().toString(),
                                                    swc.getSpouse().isEmpty(),
                                                    cwr.referenceType().isPresent(),
                                                    defaultChildTitle,
                                                    adoptedTitle,
                                                    adoptedDashes,
                                                    defaultWeight,
                                                    defaultWidth,
                                                    defaultColor,
                                                    defaultDashes);
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

    private boolean hasChildrenToExport(List<EnrichedPerson> children, Set<Integer> idsToExport) {
        return !children.isEmpty()
                && children
                .stream()
                .anyMatch(child -> idsToExport.contains(child.getId()));
    }

    private void ensurePyvisScriptPresent() throws IOException {
        Path scriptPath = properties
                .getTempDir()
                .resolve(properties.getPyvisNetworkExportScriptFilename());
        if (Files.exists(scriptPath)) {
            return;
        }
        Path familyTreeDir = properties.getTempDir().resolve("family-trees");
        Files.createDirectories(familyTreeDir);
        String resourceName = properties.getPyvisNetworkExportScriptFilename();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Pyvis script not found on classpath: " + resourceName);
            }
            Files.copy(in, scriptPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void exportToPyvisNetworkHTML(
            Path htmlPyvisNetworkFilePath,
            Path csvPyvisNetworkNodesFilePath,
            Path csvPyvisNetworkEdgesFilePath,
            String rootPersonDisplayName,
            List<Pair<String, String>> countryToColorForLegend) throws IOException {

        ensurePyvisScriptPresent();

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

        DefaultExecutor executor = DefaultExecutor.builder().get();
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
                "var width = Math.max(minWidth,maxWidth * widthFactor);",
                "var width = Math.max(10, Math.round(widthFactor*100));");
        content = content.replace(
                ".getElementById('bar').style.width = width + 'px';",
                ".getElementById('bar').style.width = width + '%';");
        content = content.replace(
                ".getElementById('bar').style.width = '496px';",
                ".getElementById('bar').style.width = '100%';");
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
        content = content.replace(
                "height:44px;",
                "height:54px;");

        String bootstrapCss = "\n    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css\" integrity=\"sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65\" crossorigin=\"anonymous\" />";
        String bootstrapJS = "\n    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js\" integrity=\"sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4\" crossorigin=\"anonymous\"></script>";
        String customCss = "\n    <style>.root-label, .country-legend { z-index:1000; } @media(max-width:767px) { .country-legend{top:42px !important;} }</style>";

        content = content.replace("</head>", bootstrapCss + customCss + "\n</head>");

        StringBuilder header = new StringBuilder()
                .append("\n    ")
                .append("<div class=\"root-label w-100 position-absolute top-0 mt-2\">")
                .append("<div class=\"text-center bg-white bg-opacity-95 mx-4 px-3 py-2 rounded shadow-sm small fw-semibold\">Árbol de: ")
                .append(escapeHtml(rootPersonDisplayName != null ? rootPersonDisplayName : "?"))
                .append("</div>")
                .append("</div>");

        if (!countryToColorForLegend.isEmpty()) {
            StringBuilder legend = new StringBuilder("<div class=\"country-legend position-absolute top-0 start-0 mt-2 ms-2 bg-white bg-opacity-95 p-2 rounded shadow-sm small\"><span class=\"d-block fw-bold mb-1\">Lugar de nacimiento</span>");
            countryToColorForLegend.forEach(ccPair -> legend
                    .append("<span class=\"d-inline-flex align-items-center me-2 mb-1\"><span class=\"d-inline-block rounded\" style=\"width:10px;height:10px;margin-right:4px;background:")
                    .append(ccPair.getRight())
                    .append(";\"></span>")
                    .append(escapeHtml(ccPair.getLeft()))
                    .append("</span>"));
            legend.append("</div>");
            header.append(legend);
        }
        content = content.replace("<body>", "<body>" + header);

        String mobileOnlyNavScript = "<script>(function(){"
                + "function applyNavOptions(){"
                + "if(typeof network==='undefined')return;"
                + "var mobile=window.matchMedia('(max-width:767px)').matches;"
                + "network.setOptions({interaction:{navigationButtons:!mobile,keyboard:!mobile}});"
                + "}"
                + "function run(){applyNavOptions();window.addEventListener('resize',applyNavOptions);}"
                + "if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',function(){setTimeout(run,100);});"
                + "else setTimeout(run,100);"
                + "})();</script>";
        content = content.replace("</body>", mobileOnlyNavScript + bootstrapJS + "\n</body>");

        Files.writeString(htmlPyvisNetworkFilePath, content, charset);
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

}
