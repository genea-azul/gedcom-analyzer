package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.PyvisNetworkMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PyvisNetworkService {

    private final PyvisNetworkMapper pyvisNetworkMapper;
    private final GedcomAnalyzerProperties properties;

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

        String[] HEADERS = {"id", "label", "title", "shape", "color", "size"};

        Map<String, String[]> countryColorsMap = Map.of(
                "Argentina", new String[] {"#9AE4FF", "#74ACDF"},
                "Italia", new String[] {"#00B65A", "#008C45"},
                "Francia", new String[] {"#0071DA", "#0055A4"},
                "España", new String[] {"#E61C21", "#AD1519"},
                "Inglaterra", new String[] {"#E61C21", "#AD1519"},
                "Irlanda", new String[] {"#E61C21", "#AD1519"},
                "Países Bajos", new String[] {"#E61C21", "#AD1519"});

        String defaultLabel = "?";
        String[] defaultColors = new String[] {"#000000", "#666666"};
        double defaultSize = 25;

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

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

        Path path = properties.getPyvisNetworkExportScriptPath();

        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);

        CommandLine commandLine = CommandLine
                .parse("py " + path.toAbsolutePath().normalize())
                .addArgument(htmlPyvisNetworkFilePath.getParent().toAbsolutePath().normalize().toString(), true)
                .addArgument(htmlPyvisNetworkFilePath.getFileName().toString(), true)
                .addArgument(csvPyvisNetworkNodesFilePath.getFileName().toString(), true)
                .addArgument(csvPyvisNetworkEdgesFilePath.getFileName().toString(), true);

        System.out.println(commandLine.toString());
        int exitCode = executor.execute(commandLine);
        System.out.println(exitCode);
        System.out.println(outputStream);
    }

}
