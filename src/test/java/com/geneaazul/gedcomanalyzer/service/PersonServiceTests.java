package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class PersonServiceTests {

    @Autowired
    private PersonService personService;
    @Autowired
    private GedcomHolder gedcomHolder;
    @Autowired
    private RelationshipMapper relationshipMapper;

    @Test
    public void testGetPeopleInTreeWhenOneParentIsBiologicalAndAdoptive() {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(9));
        List<Relationships> relationshipsList = personService.getPeopleInTree(person, false, false, true);

        List<FormattedRelationship> formattedRelationships = relationshipsList
                .stream()
                .map(Relationships::getOrderedRelationships)
                .map(TreeSet::first)
                .sorted()
                .map(relationship -> relationshipMapper.toRelationshipDto(relationship, false))
                .map(relationshipDto -> relationshipMapper.formatInSpanish(relationshipDto, false))
                .toList();

        formattedRelationships.forEach(System.out::println);

        assertThat(formattedRelationships.size()).isEqualTo(17);

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B&A")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("persona principal");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo(" ");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Father B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("padre");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("madre");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("→");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Father A")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("padre adoptivo");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Father B Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("pareja de padre");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Mother B Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("pareja de madre");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("→");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Father A Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("pareja de padre adoptivo");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        // Siblings from both biological parents, one is adopted

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B1 Father B - Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("hermano");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("↔");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son A1 Father B - Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("↔");
                });

        // Siblings from biological parent and adoptive parent, one is adopted

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B2 Father A - Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("↔");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son A2 Father A - Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("↔");
                });

        // Siblings from biological mother and new couple, one is adopted

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B3 Mother B - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("→");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son A3 Mother B - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("→");
                });

        // Siblings from biological father and new couple, one is adopted

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B4 Father B - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son A4 Father B - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        // Siblings from adoptive father and new couple, one is adopted

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B5 Father A - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son A5 Father A - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        assertThat(formattedRelationships)
                .map(FormattedRelationship::personName)
                .containsExactly(
                        "Son B&A",
                        "Father B",
                        "Mother B",
                        "Father A",
                        "Father B Couple",
                        "Mother B Couple",
                        "Father A Couple",
                        // biological children of biological parents (biological siblings)
                        "Son B1 Father B - Mother B",
                        // biological children of biological and adoptive parents (biological half siblings)
                        "Son B2 Father A - Mother B",
                        // biological children of biological parents with other couples (biological half siblings)
                        "Son B4 Father B - Couple",
                        "Son B3 Mother B - Couple",
                        // adopted children of biological parents (adoptive siblings)
                        "Son A1 Father B - Mother B",
                        // adopted children of biological and adoptive parents (adoptive siblings)
                        "Son A2 Father A - Mother B",
                        // adopted children of biological parents with other couples (adoptive half siblings)
                        "Son A4 Father B - Couple",
                        "Son A3 Mother B - Couple",
                        // biological and adoptive children of adoptive parents with other couples (adoptive half siblings)
                        "Son B5 Father A - Couple",
                        "Son A5 Father A - Couple");
    }

}
