package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
@SuppressWarnings("DataFlowIssue")
public class FamilyTreeServiceTests {

    @Autowired
    private GedcomHolder gedcomHolder;
    @Autowired
    private PersonService personService;
    @Autowired
    private FamilyTreeService familyTreeService;
    @Autowired
    private RelationshipMapper relationshipMapper;
    @Autowired
    private GedcomAnalyzerProperties properties;

    @Test
    public void testExportToPDF() throws IOException {

        EnrichedPerson person = gedcomHolder.getGedcom().getPersonById("I4");
        List<Relationship> relationships = personService.setTransientProperties(person, false);

        MutableInt index = new MutableInt(1);
        List<FormattedRelationship> peopleInTree = relationships
                .stream()
                .sorted()
                .map(relationship -> relationshipMapper.toRelationshipDto(relationship, false))
                .map(relationship -> relationshipMapper.formatInSpanish(relationship, index.getAndIncrement(), true))
                .toList();

        Path path = properties.getTempDir().resolve("export_to_pdf_test.pdf");
        familyTreeService.exportToPDF(path, person, peopleInTree);
    }

}
