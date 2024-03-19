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
public class PlainFamilyTreePdfServiceTests {

    @Autowired
    private GedcomHolder gedcomHolder;
    @Autowired
    private PlainFamilyTreePdfService plainFamilyTreePdfService;
    @Autowired
    private FamilyTreeManager familyTreeManager;
    @Autowired
    private GedcomAnalyzerProperties properties;

    @Value("${obfuscate-living:false}")
    private boolean obfuscateLiving;

    @Value("${only-secondary-description:true}")
    private boolean onlySecondaryDescription;

    @Test
    public void testExportToPDF() {

        EnrichedPerson person = gedcomHolder.getGedcom().getPersonById(4);
        assert person != null;

        List<List<Relationship>> relationshipsList = familyTreeManager.getRelationshipsWithNotInLawPriority(person);

        Path path = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve("export_to_pdf_test.pdf");

        plainFamilyTreePdfService.export(
                path,
                person,
                obfuscateLiving,
                onlySecondaryDescription,
                relationshipsList);
    }

}
