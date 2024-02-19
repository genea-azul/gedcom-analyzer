package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class NetworkFamilyTreeServiceTests {

    @Autowired
    private GedcomHolder gedcomHolder;
    @Autowired
    private NetworkFamilyTreeService networkFamilyTreeService;
    @Autowired
    private FamilyTreeManager familyTreeManager;
    @Autowired
    private GedcomAnalyzerProperties properties;

    @Value("${obfuscate-condition:false}")
    private boolean obfuscateCondition;

    @Test
    public void testGenerateNetworkHTML() {

        EnrichedPerson person = gedcomHolder.getGedcom().getPersonById(4);
        assert person != null;

        List<List<Relationship>> relationshipsList = familyTreeManager.getRelationshipsWithNotInLawPriority(person);

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

        networkFamilyTreeService.generate(
                htmlPyvisNetworkFilePath,
                csvPyvisNetworkNodesFilePath,
                csvPyvisNetworkEdgesFilePath,
                false,
                relationshipsList);
    }

}
