package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.service.PersonService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class PlainFamilyTreeTxtServiceTests {

    @Autowired
    private GedcomHolder gedcomHolder;
    @Autowired
    private PersonService personService;
    @Autowired
    private PlainFamilyTreeTxtService plainFamilyTreeTxtService;
    @Autowired
    private RelationshipMapper relationshipMapper;
    @Autowired
    private GedcomAnalyzerProperties properties;

    @Value("${obfuscate-condition:false}")
    private boolean obfuscateCondition;

    @Test
    public void testExportToTXT() throws IOException {

        EnrichedPerson person = gedcomHolder.getGedcom().getPersonById("I4");
        assert person != null;

        List<Relationships> relationshipsList = personService.setTransientProperties(person, false);

        MutableInt index = new MutableInt(1);
        List<FormattedRelationship> peopleInTree = relationshipsList
                .stream()
                .map(Relationships::findLast)
                .sorted()
                .peek(relationship -> relationship.person().setOrderKey(index.getAndIncrement()))
                .map(relationship -> relationshipMapper.toRelationshipDto(relationship, obfuscateCondition))
                .map(relationship -> relationshipMapper.formatInSpanish(relationship, false))
                .toList();

        Path path = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve("export_to_txt_test.txt");
        plainFamilyTreeTxtService.exportToTXT(path, person, peopleInTree);
    }

}
