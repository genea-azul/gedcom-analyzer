package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.service.familytree.NetworkFamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class NetworkFamilyTreeServiceTests {

    @Autowired
    private GedcomHolder gedcomHolder;
    @Autowired
    private PersonService personService;
    @Autowired
    private NetworkFamilyTreeService networkFamilyTreeService;
    @Autowired
    private GedcomAnalyzerProperties properties;

    @Test
    public void testGenerateNetworkHTML() throws IOException {

        EnrichedPerson person = gedcomHolder.getGedcom().getPersonById("I4");
        List<Relationships> relationshipsList = personService.setTransientProperties(person, false);

        MutableInt index = new MutableInt(1);
        List<EnrichedPerson> peopleInTree = relationshipsList
                .stream()
                .map(Relationships::findLast)
                .sorted()
                .limit(100)
                .map(Relationship::person)
                .peek(p -> p.setOrderKey(index.getAndIncrement()))
                .toList();

        Path htmlPyvisNetworkFilePath = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve("export_to_html_test.html");

        Path csvPyvisNetworkNodesFilePath = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve("export_to_csv_nodes_test.csv");

        Path csvPyvisNetworkEdgesFilePath = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve("export_to_csv_edges_test.csv");

        networkFamilyTreeService.generateNetworkHTML(
                htmlPyvisNetworkFilePath,
                csvPyvisNetworkNodesFilePath,
                csvPyvisNetworkEdgesFilePath,
                false,
                peopleInTree);
    }

}
