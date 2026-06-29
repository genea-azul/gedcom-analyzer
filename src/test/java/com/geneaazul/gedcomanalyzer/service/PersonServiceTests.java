package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.model.dto.AdoptionType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
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
                        // I10 sorts before I12: all fields are equal except relatedPersonIds ([6,7] < [6,8])
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
    public void getRelationshipBetween_adoptiveSibling_returnsFullSiblingRelationshipWithAdoptiveDesc() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        // I9 (Son B&A) is the biological child of F4 (I7 Father B + I6 Mother B).
        // I10 (Son A1) is the adopted child of the same F4 family.
        // Both share the same two parents → isHalf=false, but I10's referenceType is ADOPTIVE → adoptionTypeDesc=ADOPTIVE.
        EnrichedPerson biological = Objects.requireNonNull(gedcom.getPersonById(9));   // Son B&A (biological in F4)
        EnrichedPerson adopted    = Objects.requireNonNull(gedcom.getPersonById(10));  // Son A1 Father B - Mother B (adopted in F4)

        Relationship relationship = personService.getRelationshipBetween(biological, adopted);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(adopted.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isEqualTo(1);
        assertThat(relationship.distanceToAncestorThisPerson()).isEqualTo(1);
        assertThat(relationship.isHalf()).isFalse();
        assertThat(relationship.isInLaw()).isFalse();
        assertThat(relationship.adoptionTypeDesc()).isEqualTo(AdoptionType.ADOPTIVE);
    }

    @Test
    public void getRelationshipBetween_nonDirectRelationship_returnsNull() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson grandparent = Objects.requireNonNull(gedcom.getPersonById(1)); // Test Father (I1)
        EnrichedPerson grandchild = Objects.requireNonNull(gedcom.getPersonById(5));  // Test Grandson (I5) - child of I3

        Relationship relationship = personService.getRelationshipBetween(grandparent, grandchild);

        assertThat(relationship).isNull();
    }

    @Test
    public void getRelationshipBetween_reverseNonDirectRelationship_returnsNull() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        // Same pair in the opposite direction: grandchild→grandparent is also non-direct
        EnrichedPerson grandchild = Objects.requireNonNull(gedcom.getPersonById(5));  // Test Grandson (I5) - child of I3
        EnrichedPerson grandparent = Objects.requireNonNull(gedcom.getPersonById(1)); // Test Father (I1)

        Relationship relationship = personService.getRelationshipBetween(grandchild, grandparent);

        assertThat(relationship).isNull();
    }

    @Test
    public void getRelationshipBetween_adoptiveParent_returnsAdoptiveParentRelationship() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        // I9 (Son B&A) is the biological child of I7+I6 (F4) and the *adopted* child of I8+I6 (F5)
        EnrichedPerson adoptedChild = Objects.requireNonNull(gedcom.getPersonById(9));  // Son B&A (I9)
        EnrichedPerson adoptiveParent = Objects.requireNonNull(gedcom.getPersonById(8)); // Father A (I8)

        Relationship relationship = personService.getRelationshipBetween(adoptedChild, adoptiveParent);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(adoptiveParent.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isEqualTo(1);
        assertThat(relationship.distanceToAncestorThisPerson()).isEqualTo(0);
        assertThat(relationship.isInLaw()).isFalse();
        assertThat(relationship.adoptionTypeAsc()).isEqualTo(AdoptionType.ADOPTIVE);
    }

    @Test
    public void getRelationshipBetween_adoptiveChild_returnsAdoptiveChildRelationship() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        // Reverse of getRelationshipBetween_adoptiveParent: I8 (Father A) → I9 (Son B&A, PEDI Adopted in F5).
        // Exercises the childWithReference branch with a non-null referenceType (ADOPTED_CHILD),
        // distinct from the biological getRelationshipBetween_childParent test.
        EnrichedPerson adoptiveParent = Objects.requireNonNull(gedcom.getPersonById(8)); // Father A (I8)
        EnrichedPerson adoptedChild   = Objects.requireNonNull(gedcom.getPersonById(9)); // Son B&A (I9)

        Relationship relationship = personService.getRelationshipBetween(adoptiveParent, adoptedChild);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(adoptedChild.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isZero();
        assertThat(relationship.distanceToAncestorThisPerson()).isEqualTo(1);
        assertThat(relationship.isInLaw()).isFalse();
        assertThat(relationship.adoptionTypeDesc()).isEqualTo(AdoptionType.ADOPTIVE);
    }

    // ── getPeopleInTree ───────────────────────────────────────────────────────

    @Test
    public void getPeopleInTree_onlyAscDirection_returnsOnlyAncestorsNoSpouses() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        // I9 has two biological parents (I7 Father B, I6 Mother B) and one adoptive parent (I8 Father A).
        // Each parent also has a second spouse in the gedcom (I15, I14, I16) — those must NOT appear.
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(9)); // Son B&A (I9)

        List<Relationships> relationshipsList = personService.getPeopleInTree(person, false, true, false);

        List<String> names = relationshipsList.stream()
                .map(Relationships::findFirst)
                .map(r -> r.person().getDisplayName())
                .toList();

        names.forEach(System.out::println);

        // Self + 3 parents only
        assertThat(relationshipsList).hasSize(4);

        // All returned persons are ancestors (desc == 0)
        assertThat(relationshipsList)
                .allSatisfy(rel ->
                        assertThat(rel.findFirst().distanceToAncestorThisPerson()).isZero());

        // Parents of parents are not in the gedcom → max asc depth = 1
        assertThat(relationshipsList)
                .allSatisfy(rel ->
                        assertThat(rel.findFirst().distanceToAncestorRootPerson()).isLessThanOrEqualTo(1));

        // Spouses of parents must not be included
        List<Integer> ids = relationshipsList.stream()
                .map(Relationships::getPersonId)
                .toList();
        assertThat(ids).doesNotContain(15); // Father B Couple (I15, spouse of I7)
        assertThat(ids).doesNotContain(14); // Mother B Couple (I14, spouse of I6 in F6)
        assertThat(ids).doesNotContain(16); // Father A Couple (I16, spouse of I8)
    }

    @Test
    public void getPeopleInTree_onlyAscDirection_withExcludeRootPerson_returnsOnlyAncestors() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(9)); // Son B&A (I9)

        List<Relationships> relationshipsList = personService.getPeopleInTree(person, true, true, false);

        // Root person excluded, 3 parents remain
        assertThat(relationshipsList).hasSize(3);

        // All are direct ancestors (desc == 0, asc == 1)
        assertThat(relationshipsList).allSatisfy(rel -> {
            assertThat(rel.findFirst().distanceToAncestorThisPerson()).isZero();
            assertThat(rel.findFirst().distanceToAncestorRootPerson()).isEqualTo(1);
        });
    }

    @Test
    public void getPeopleInTree_bidirectional_returnsSpouseAndDescendants() {
        // I1 has no parents in the GEDCOM, spouse I2 (via F1), daughter I3 (F1), son I4 (F2, no
        // mother), and grandchild I5 (via I3's family F3, no co-parent). This exercises the full
        // ASC+SAME+DESC traversal from a root with no ancestors.
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(1)); // Test Father (I1)

        List<Relationships> result = personService.getPeopleInTree(person, false, false, false);

        assertThat(result.stream().map(Relationships::getPersonId).toList())
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5);

        assertThat(findById(result, 2)).satisfies(r -> {
            assertThat(r.getDistance()).isZero();
            assertThat(r.isInLaw()).isTrue();
        });
        assertThat(findById(result, 3)).satisfies(r -> {
            assertThat(r.distanceToAncestorRootPerson()).isZero();
            assertThat(r.distanceToAncestorThisPerson()).isEqualTo(1);
            assertThat(r.isHalf()).isFalse();
        });
        assertThat(findById(result, 4)).satisfies(r -> {
            assertThat(r.distanceToAncestorRootPerson()).isZero();
            assertThat(r.distanceToAncestorThisPerson()).isEqualTo(1);
            assertThat(r.isHalf()).isFalse();
        });
        assertThat(findById(result, 5)).satisfies(r -> {
            assertThat(r.distanceToAncestorRootPerson()).isZero();
            assertThat(r.distanceToAncestorThisPerson()).isEqualTo(2);
        });
    }

    @Test
    public void getPeopleInTree_bidirectional_descendantAndSpouseTreeSides() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        // I1 has no parents in the GEDCOM, so every non-self node is a spouse or descendant.
        // Verifies that the orElseGet(() -> Set.of(SPOUSE)) / Set.of(DESCENDANT) defaults in
        // resolveRelativesToTraverse fire correctly, and that grandchildren inherit DESCENDANT.
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(1)); // Test Father (I1)

        List<Relationships> result = personService.getPeopleInTree(person, false, false, true);

        assertThat(findById(result, 2).treeSides()).containsExactly(TreeSideType.SPOUSE);
        assertThat(findById(result, 3).treeSides()).containsExactly(TreeSideType.DESCENDANT);
        assertThat(findById(result, 4).treeSides()).containsExactly(TreeSideType.DESCENDANT);
        assertThat(findById(result, 5).treeSides()).containsExactly(TreeSideType.DESCENDANT);
    }

    @Test
    public void getPeopleInTree_bidirectional_traversesUpThenLaterally() {
        // Starting from I3 the traversal goes ASC to I1 and I2 (parents via F1), DESC to I5
        // (I3's child in F3), and then from I1 continues DESC to reach I4 (I1's child in F2,
        // no mother). I4 is a half-sibling of I3 because they share only I1.
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(3)); // Test Daughter (I3)

        List<Relationships> result = personService.getPeopleInTree(person, false, false, false);

        assertThat(result.stream().map(Relationships::getPersonId).toList())
                .containsExactlyInAnyOrder(3, 1, 2, 4, 5);

        assertThat(findById(result, 1)).satisfies(r -> {
            assertThat(r.distanceToAncestorRootPerson()).isEqualTo(1);
            assertThat(r.distanceToAncestorThisPerson()).isZero();
            assertThat(r.isInLaw()).isFalse();
        });
        assertThat(findById(result, 2)).satisfies(r -> {
            assertThat(r.distanceToAncestorRootPerson()).isEqualTo(1);
            assertThat(r.distanceToAncestorThisPerson()).isZero();
            assertThat(r.isInLaw()).isFalse();
        });
        assertThat(findById(result, 5)).satisfies(r -> {
            assertThat(r.distanceToAncestorRootPerson()).isZero();
            assertThat(r.distanceToAncestorThisPerson()).isEqualTo(1);
        });
        // I4 is I3's half-sibling: reachable only via I1, shares only the paternal side
        assertThat(findById(result, 4)).satisfies(r -> {
            assertThat(r.distanceToAncestorRootPerson()).isEqualTo(1);
            assertThat(r.distanceToAncestorThisPerson()).isEqualTo(1);
            assertThat(r.isHalf()).isTrue();
        });
    }

    // ── stopTraversing conditions ─────────────────────────────────────────────

    @Test
    public void getPeopleInTree_withPreCondition_personAndAllPathsThroughItAreBlocked() {
        // Starting from I3, block I1 via pre-condition.
        // I4 is only reachable through I1 (via F2, no mother), so both I1 and I4 must be absent.
        // I2 is a direct parent of I3 in F1, so it remains reachable without going through I1.
        // Contrast with the post-condition test below: pre-condition removes the person itself.
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(3)); // Test Daughter (I3)

        List<Relationships> result = personService.getPeopleInTree(
                person, false, false, false,
                (p, distance) -> p.getId().equals(1),
                (p, distance) -> false);

        assertThat(result.stream().map(Relationships::getPersonId).toList())
                .containsExactlyInAnyOrder(3, 2, 5);
    }

    @Test
    public void getPeopleInTree_withPostCondition_personIsAddedButRelativesNotExpanded() {
        // Same setup, but block expansion FROM I1 via post-condition instead.
        // I1 itself IS added to the result (post-condition fires after the person is stored),
        // but I4 remains absent because I1's relatives are never expanded.
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(3)); // Test Daughter (I3)

        List<Relationships> result = personService.getPeopleInTree(
                person, false, false, false,
                (p, distance) -> false,
                (p, distance) -> p.getId().equals(1));

        assertThat(result.stream().map(Relationships::getPersonId).toList())
                .containsExactlyInAnyOrder(3, 1, 2, 5);
    }

    // ── mergeTreeSides / endogamy ─────────────────────────────────────────────

    @Test
    public void getPeopleInTree_endogamousAncestor_mergesTreeSidesFromBothLines() {
        // I32 has great-grandparents I24 (male) and I25 (female) who appear on BOTH the paternal
        // line (I32→I28→I26→I24+I25) and the maternal line (I32→I31→I29→I24+I25).
        // With mergeTreeSides=true, the traversal re-encounters I24/I25 via the second path and
        // must propagate the new tree side, resulting in both FATHER and MOTHER for those two nodes.
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(32)); // Root Person (I32)

        List<Relationships> result = personService.getPeopleInTree(person, true, true, true);

        assertThat(result.stream().map(Relationships::getPersonId).toList())
                .containsExactlyInAnyOrder(28, 31, 26, 27, 29, 30, 24, 25);

        // Paternal-line-only ancestors carry only FATHER
        assertThat(findById(result, 28).treeSides()).containsExactly(TreeSideType.FATHER);
        assertThat(findById(result, 26).treeSides()).containsExactly(TreeSideType.FATHER);
        assertThat(findById(result, 27).treeSides()).containsExactly(TreeSideType.FATHER);

        // Maternal-line-only ancestors carry only MOTHER
        assertThat(findById(result, 31).treeSides()).containsExactly(TreeSideType.MOTHER);
        assertThat(findById(result, 29).treeSides()).containsExactly(TreeSideType.MOTHER);
        assertThat(findById(result, 30).treeSides()).containsExactly(TreeSideType.MOTHER);

        // Common great-grandparents carry both sides after mergeTreeSides cascade
        assertThat(findById(result, 24).treeSides())
                .containsExactlyInAnyOrder(TreeSideType.FATHER, TreeSideType.MOTHER);
        assertThat(findById(result, 25).treeSides())
                .containsExactlyInAnyOrder(TreeSideType.FATHER, TreeSideType.MOTHER);
    }

    @Test
    public void getPeopleInTree_endogamousAncestor_withoutMergeTreeSides_keepsFirstEncounteredSide() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        // Same diamond as mergesTreeSidesFromBothLines, but mergeTreeSides=false.
        // The paternal line (I32→I28→I26→I24/I25) is traversed first in DFS order, so I24 and I25
        // get FATHER. When the maternal line reaches them, no merging happens — they keep FATHER only.
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(32)); // Root Person (I32)

        List<Relationships> result = personService.getPeopleInTree(person, true, true, false);

        assertThat(findById(result, 24).treeSides()).containsExactly(TreeSideType.FATHER);
        assertThat(findById(result, 25).treeSides()).containsExactly(TreeSideType.FATHER);
    }

    // ── foster adoption ───────────────────────────────────────────────────────

    @Test
    public void getRelationshipBetween_fosterParent_returnsFosterAdoptionType() {
        // I33 is a foster child of I34 (PEDI Foster in F13). AdoptionType.FOSTER is distinct
        // from AdoptionType.ADOPTIVE and exercises the FOSTER_PARENT/FOSTER_CHILD branch of
        // resolveAdoptionType(), which is otherwise untested end-to-end.
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson fosterChild = Objects.requireNonNull(gedcom.getPersonById(33));  // Foster Child (I33)
        EnrichedPerson fosterParent = Objects.requireNonNull(gedcom.getPersonById(34)); // Foster Parent (I34)

        Relationship relationship = personService.getRelationshipBetween(fosterChild, fosterParent);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(fosterParent.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isEqualTo(1);
        assertThat(relationship.distanceToAncestorThisPerson()).isZero();
        assertThat(relationship.adoptionTypeAsc()).isEqualTo(AdoptionType.FOSTER);
    }

    @Test
    public void getRelationshipBetween_fosterChild_returnsFosterChildRelationship() {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        // Reverse of getRelationshipBetween_fosterParent: I34 (Foster Parent) → I33 (Foster Child, PEDI Foster in F13).
        // Exercises the childWithReference branch with referenceType=FOSTER_CHILD.
        EnrichedPerson fosterParent = Objects.requireNonNull(gedcom.getPersonById(34)); // Foster Parent (I34)
        EnrichedPerson fosterChild  = Objects.requireNonNull(gedcom.getPersonById(33)); // Foster Child (I33)

        Relationship relationship = personService.getRelationshipBetween(fosterParent, fosterChild);

        assertThat(relationship).isNotNull();
        assertThat(relationship.person().getId()).isEqualTo(fosterChild.getId());
        assertThat(relationship.distanceToAncestorRootPerson()).isZero();
        assertThat(relationship.distanceToAncestorThisPerson()).isEqualTo(1);
        assertThat(relationship.isInLaw()).isFalse();
        assertThat(relationship.adoptionTypeDesc()).isEqualTo(AdoptionType.FOSTER);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Relationship findById(List<Relationships> result, int id) {
        return result.stream()
                .filter(rels -> rels.getPersonId().equals(id))
                .map(Relationships::findFirst)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Person " + id + " not found in result"));
    }
}
