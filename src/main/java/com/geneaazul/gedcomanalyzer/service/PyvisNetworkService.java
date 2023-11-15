package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.PyvisNetworkMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.stereotype.Service;

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

    public void generateNetworkHTML(
            Path path,
            EnrichedPerson person,
            List<EnrichedPerson> peopleInTree) throws IOException {

        MutableInt orderCount = new MutableInt(0);
        List<EnrichedPerson> peopleToExport = peopleInTree
                .stream()
                .limit(1200)
                .peek(p -> p.setOrderKey(orderCount.getAndIncrement()))
                .toList();

        Path nodesPath = path
                .getParent()
                .resolve("test_pyvis_nodes_export.csv");
        exportToPyvisNodesCSV(nodesPath, peopleToExport);

        Path edgesPath = path
                .getParent()
                .resolve("test_pyvis_edges_export.csv");
        exportToPyvisEdgesCSV(edgesPath, peopleToExport);
    }

    public void exportToPyvisNodesCSV(Path path, List<EnrichedPerson> people) throws IOException {

        String[] HEADERS = {"id", "label", "title", "color", "size"};

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

}
