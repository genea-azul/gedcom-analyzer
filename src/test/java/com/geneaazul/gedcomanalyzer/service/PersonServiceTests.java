package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

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

        /*
         *                                         /───────[F6]─────────────────────────┬── I14
         * I15 ──[F7]─┬──── I7 ───[F4]─┬─────── I6 ──────┬─[F5]─── I8 ────┬─[F8]── I16  |
         *            |                |                 |                |             |
         *        ┌───┴──┐      ┌──────┼──────┐   ┌──────┼──────┐      ┌──┴───┐      ┌──┴───┐
         *       [A]     |     [A]     |       \ [A]    [A]     |     [A]     |     [A]     |
         *        |      |      |      |        |        |      |      |      |      |      |
         *       I19    I20    I10    I11      I9       I12    I13    I21    I22    I17    I18
         */

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(9));
        List<Relationships> relationshipsList = personService.getPeopleInTree(person, false, false, true);

        List<FormattedRelationship> formattedRelationships = relationshipsList
                .stream()
                .map(Relationships::findFirst) // for the sake of this test it doesn't actually matter the order
                .sorted()
                .map(relationship -> relationshipMapper.toRelationshipDto(relationship, false))
                .map(relationshipDto -> relationshipMapper.formatInSpanish(relationshipDto, false))
                .toList();

        formattedRelationships.forEach(System.out::println);

        assertThat(formattedRelationships.size()).isEqualTo(17);

        // I9
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B&A")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("persona principal");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo(" ");
                });

        // I6
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Father B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("padre");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        // I7
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("madre");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("→");
                });

        // I8
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Father A")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("padre adoptivo");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        // I15
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Father B Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("pareja de padre");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        // I14
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Mother B Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("pareja de madre");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("→");
                });

        // I16
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Father A Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("pareja de padre adoptivo");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        // Siblings from both biological parents, one is adopted

        // I11
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B1 Father B - Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("hermano");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("↔");
                });

        // I10
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son A1 Father B - Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("↔");
                });

        // Siblings from biological parent and adoptive parent, one is adopted

        // I13
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B2 Father A - Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("↔");
                });

        // I12
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son A2 Father A - Mother B")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("↔");
                });

        // Siblings from biological mother and new couple, one is adopted

        // I18
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B3 Mother B - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("→");
                });

        // I17
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son A3 Mother B - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("→");
                });

        // Siblings from biological father and new couple, one is adopted

        // I20
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B4 Father B - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isNull();
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        // I19
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son A4 Father B - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        // Siblings from adoptive father and new couple, one is adopted

        // I22
        assertThat(formattedRelationships.stream().filter(f -> f.personName().equals("Son B5 Father A - Couple")).findFirst())
                .hasValueSatisfying(f -> {
                    assertThat(f.relationshipDesc()).isEqualTo("medio-hermano");
                    assertThat(f.adoption()).isEqualTo("ADOPTIVE");
                    assertThat(f.treeSide()).isEqualTo("←");
                });

        // I21
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
                        "Son A1 Father B - Mother B", // TODO check why sometimes test result switches lines
                        // adopted children of biological and adoptive parents (adoptive siblings)
                        "Son A2 Father A - Mother B", // TODO check why sometimes test result switches lines
                        // adopted children of biological parents with other couples (adoptive half siblings)
                        "Son A4 Father B - Couple",
                        "Son A3 Mother B - Couple",
                        // biological and adoptive children of adoptive parents with other couples (adoptive half siblings)
                        "Son B5 Father A - Couple",
                        "Son A5 Father A - Couple");
    }

    @Test
    public void getRelationshipBetween_parentChild_returnsParentRelationship() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson child = Objects.requireNonNull(gedcom.getPersonById(3));   // Test Dauther (I3)
        EnrichedPerson parent = Objects.requireNonNull(gedcom.getPersonById(1));   // Test Father (I1)

        Relationship relationship = personService.getRelationshipBetween(child, parent);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(parent.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isEqualTo(1);
        assertThat(relationship.distanceToAncestorThisPerson()).isEqualTo(0);
        assertThat(relationship.isHalf()).isFalse();
        assertThat(relationship.isInLaw()).isFalse();
    }

    @Test
    public void getRelationshipBetween_childParent_returnsChildRelationship() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson parent = Objects.requireNonNull(gedcom.getPersonById(1));   // Test Father (I1)
        EnrichedPerson child = Objects.requireNonNull(gedcom.getPersonById(3));   // Test Dauther (I3)

        Relationship relationship = personService.getRelationshipBetween(parent, child);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(child.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isEqualTo(0);
        assertThat(relationship.distanceToAncestorThisPerson()).isEqualTo(1);
        assertThat(relationship.isHalf()).isFalse();
        assertThat(relationship.isInLaw()).isFalse();
    }

    @Test
    public void getRelationshipBetween_spouses_returnsSpouseRelationship() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson personA = Objects.requireNonNull(gedcom.getPersonById(1));  // Test Father (I1)
        EnrichedPerson personB = Objects.requireNonNull(gedcom.getPersonById(2)); // Test Mother (I2)

        Relationship relationship = personService.getRelationshipBetween(personA, personB);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(personB.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isEqualTo(0);
        assertThat(relationship.distanceToAncestorThisPerson()).isEqualTo(0);
        assertThat(relationship.isInLaw()).isTrue();
        assertThat(relationship.isHalf()).isFalse();
    }

    @Test
    public void getRelationshipBetween_halfSiblings_returnsHalfSiblingRelationship() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson personA = Objects.requireNonNull(gedcom.getPersonById(3)); // Test Dauther (I3) - F1: I1,I2
        EnrichedPerson personB = Objects.requireNonNull(gedcom.getPersonById(4)); // Test Other Son (I4) - F2: I1 only

        Relationship relationship = personService.getRelationshipBetween(personA, personB);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(personB.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isEqualTo(1);
        assertThat(relationship.distanceToAncestorThisPerson()).isEqualTo(1);
        assertThat(relationship.isHalf()).isTrue();
        assertThat(relationship.isInLaw()).isFalse();
    }

    @Test
    public void getRelationshipBetween_fullSiblings_returnsFullSiblingRelationship() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson personA = Objects.requireNonNull(gedcom.getPersonById(9));  // Son B&A (I9)
        EnrichedPerson personB = Objects.requireNonNull(gedcom.getPersonById(11)); // Son B1 Father B - Mother B (I11)
        // Same parents in F4: I7 (Father B), I6 (Mother B)

        Relationship relationship = personService.getRelationshipBetween(personA, personB);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(personB.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isEqualTo(1);
        assertThat(relationship.distanceToAncestorThisPerson()).isEqualTo(1);
        assertThat(relationship.isHalf()).isFalse();
        assertThat(relationship.isInLaw()).isFalse();
    }

    @Test
    public void getRelationshipBetween_nonDirectRelationship_returnsNull() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson grandparent = Objects.requireNonNull(gedcom.getPersonById(1)); // Test Father (I1)
        EnrichedPerson grandchild = Objects.requireNonNull(gedcom.getPersonById(5));  // Test Grandson (I5) - child of I3

        Relationship relationship = personService.getRelationshipBetween(grandparent, grandchild);

        assertThat(relationship).isNull();
    }
}
