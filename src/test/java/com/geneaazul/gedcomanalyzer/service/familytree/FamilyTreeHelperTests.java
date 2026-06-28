package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.service.PersonService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyTreeHelperTests {

    @Mock
    private com.geneaazul.gedcomanalyzer.service.PersonService personService;

    @InjectMocks
    private FamilyTreeHelper familyTreeHelper;

    @Test
    void getFamilyTreeFileId_withGivenNameAndSurname_returnsConcatenatedSimplified() {
        EnrichedPerson person = org.mockito.Mockito.mock(EnrichedPerson.class);
        when(person.getGivenName()).thenReturn(Optional.of(GivenName.of("Juan", "juan", "juan")));
        when(person.getSurname()).thenReturn(Optional.of(Surname.of("Pérez", "perez", "perez", "perez")));
        assertThat(familyTreeHelper.getFamilyTreeFileId(person)).isEqualTo("juan_perez");
    }

    @Test
    void getFamilyTreeFileId_withSpacesInNames_replacesWithUnderscores() {
        EnrichedPerson person = org.mockito.Mockito.mock(EnrichedPerson.class);
        when(person.getGivenName()).thenReturn(Optional.of(GivenName.of("Mary Jane", "mary jane", "mary jane")));
        when(person.getSurname()).thenReturn(Optional.of(Surname.of("Van Der Berg", "van der berg", "berg", "berg_")));
        assertThat(familyTreeHelper.getFamilyTreeFileId(person)).isEqualTo("mary_jane_van_der_berg");
    }

    @Test
    void getFamilyTreeFileId_withOnlyGivenName_returnsSimplifiedGivenName() {
        EnrichedPerson person = org.mockito.Mockito.mock(EnrichedPerson.class);
        when(person.getGivenName()).thenReturn(Optional.of(GivenName.of("Anonymous", "anonymous", "anonymous")));
        when(person.getSurname()).thenReturn(Optional.empty());
        assertThat(familyTreeHelper.getFamilyTreeFileId(person)).isEqualTo("anonymous");
    }

    @Test
    void getFamilyTreeFileId_withNoName_returnsDefault() {
        EnrichedPerson person = org.mockito.Mockito.mock(EnrichedPerson.class);
        when(person.getGivenName()).thenReturn(Optional.empty());
        when(person.getSurname()).thenReturn(Optional.empty());
        assertThat(familyTreeHelper.getFamilyTreeFileId(person)).isEqualTo("genea-azul");
    }

    // ── getRelationshipsWithNotInLawPriority(person, includeSpouseAncestors) ──

    @Test
    void getRelationshipsWithNotInLawPriority_includeSpouseAncestors_false_doesNotAddSpouseAncestors() {
        EnrichedPerson rootPerson = mockPerson(1);
        EnrichedPerson spouse     = mockPerson(2);

        Relationships rootRel   = Relationships.from(rel(rootPerson, 0, 0, false));
        Relationships spouseRel = Relationships.from(rel(spouse,     0, 0, true));

        when(personService.setTransientProperties(rootPerson, false)).thenReturn(List.of(rootRel, spouseRel));

        // getSpouses() / getPeopleInTree(spouse, ...) are not called when includeSpouseAncestors=false
        List<List<Relationship>> result = familyTreeHelper.getRelationshipsWithNotInLawPriority(rootPerson, false);

        Set<Integer> ids = ids(result);
        assertThat(ids).containsExactlyInAnyOrder(1, 2);
        assertThat(ids).doesNotContain(3);
    }

    @Test
    void getRelationshipsWithNotInLawPriority_includeSpouseAncestors_true_addsSpouseAncestorsAsInLaw() {
        EnrichedPerson rootPerson   = mockPerson(1);
        EnrichedPerson spouse       = mockPerson(2);
        EnrichedPerson spouseParent = mockPerson(3);
        EnrichedPerson spouseGrandParent = mockPerson(4);

        when(rootPerson.getSpouses()).thenReturn(List.of(spouse));

        Relationships rootRel   = Relationships.from(rel(rootPerson, 0, 0, false));
        Relationships spouseRel = Relationships.from(rel(spouse,     0, 0, true));

        // Spouse's ancestor-only traversal: spouse at distance=0, then parent at asc=1, grandparent at asc=2
        Relationships spouseAtOrigin    = Relationships.from(rel(spouse,            0, 0, false));
        Relationships spouseParentRel   = Relationships.from(rel(spouseParent,      1, 0, false));
        Relationships spouseGrandParentRel = Relationships.from(rel(spouseGrandParent, 2, 0, false));

        when(personService.setTransientProperties(rootPerson, false)).thenReturn(List.of(rootRel, spouseRel));
        when(personService.getPeopleInTree(spouse, true, true, true)).thenReturn(List.of(spouseAtOrigin, spouseParentRel, spouseGrandParentRel));

        List<List<Relationship>> result = familyTreeHelper.getRelationshipsWithNotInLawPriority(rootPerson, true);

        Set<Integer> ids = ids(result);
        assertThat(ids).containsExactlyInAnyOrder(1, 2, 3, 4);

        // Spouse's ancestors must be tagged as in-law, as spouse-family, with spouse's ID in relatedPersonIds
        assertThat(result).anySatisfy(group ->
                assertThat(group.getFirst()).satisfies(r -> {
                    assertThat(r.person().getId()).isEqualTo(3);
                    assertThat(r.isInLaw()).isTrue();
                    assertThat(r.isSpouseFamily()).isTrue();
                    assertThat(r.relatedPersonIds()).containsExactly(2);
                    assertThat(r.distanceToAncestorRootPerson()).isEqualTo(1);
                    assertThat(r.distanceToAncestorThisPerson()).isEqualTo(0);
                }));
        assertThat(result).anySatisfy(group ->
                assertThat(group.getFirst()).satisfies(r -> {
                    assertThat(r.person().getId()).isEqualTo(4);
                    assertThat(r.isInLaw()).isTrue();
                    assertThat(r.isSpouseFamily()).isTrue();
                    assertThat(r.relatedPersonIds()).containsExactly(2);
                    assertThat(r.distanceToAncestorRootPerson()).isEqualTo(2);
                    assertThat(r.distanceToAncestorThisPerson()).isEqualTo(0);
                }));
    }

    @Test
    void getRelationshipsWithNotInLawPriority_includeSpouseAncestors_doesNotDuplicateBloodRelatives() {
        // If spouse's ancestor is already a blood relative of root person, skip them
        EnrichedPerson rootPerson   = mockPerson(1);
        EnrichedPerson spouse       = mockPerson(2);
        EnrichedPerson sharedPerson = mockPerson(3); // blood relative AND ancestor of spouse

        when(rootPerson.getSpouses()).thenReturn(List.of(spouse));

        Relationships rootRel         = Relationships.from(rel(rootPerson,   0, 0, false));
        Relationships spouseRel       = Relationships.from(rel(spouse,       0, 0, true));
        Relationships sharedBloodRel  = Relationships.from(rel(sharedPerson, 1, 1, false)); // e.g. sibling

        Relationships spouseAtOrigin  = Relationships.from(rel(spouse,       0, 0, false));
        Relationships sharedAsAncestorOfSpouse = Relationships.from(rel(sharedPerson, 1, 0, false));

        when(personService.setTransientProperties(rootPerson, false)).thenReturn(List.of(rootRel, spouseRel, sharedBloodRel));
        when(personService.getPeopleInTree(spouse, true, true, true)).thenReturn(List.of(spouseAtOrigin, sharedAsAncestorOfSpouse));

        List<List<Relationship>> result = familyTreeHelper.getRelationshipsWithNotInLawPriority(rootPerson, true);

        Set<Integer> ids = ids(result);
        assertThat(ids).containsExactlyInAnyOrder(1, 2, 3);

        // sharedPerson must keep the blood relationship (isInLaw=false), not be overridden
        assertThat(result).anySatisfy(group ->
                assertThat(group.getFirst()).satisfies(r -> {
                    assertThat(r.person().getId()).isEqualTo(3);
                    assertThat(r.isInLaw()).isFalse();
                }));
    }

    @Test
    void getRelationshipsWithNotInLawPriority_includeSpouseAncestors_deduplicatesAcrossMultipleSpouses() {
        EnrichedPerson rootPerson     = mockPerson(1);
        EnrichedPerson spouse1        = mockPerson(2);
        EnrichedPerson spouse2        = mockPerson(3);
        EnrichedPerson sharedAncestor = mockPerson(4); // ancestor of both spouses

        // Pre-compute before when() calls to avoid Mockito UnfinishedStubbing
        Relationships rootRel      = Relationships.from(rel(rootPerson,     0, 0, false));
        Relationships spouse1Rel   = Relationships.from(rel(spouse1,        0, 0, true));
        Relationships spouse2Rel   = Relationships.from(rel(spouse2,        0, 0, true));
        Relationships sp1Origin    = Relationships.from(rel(spouse1,        0, 0, false));
        Relationships sp1Ancestor  = Relationships.from(rel(sharedAncestor, 1, 0, false));
        Relationships sp2Origin    = Relationships.from(rel(spouse2,        0, 0, false));
        Relationships sp2Ancestor  = Relationships.from(rel(sharedAncestor, 1, 0, false));

        when(rootPerson.getSpouses()).thenReturn(List.of(spouse1, spouse2));

        when(personService.setTransientProperties(rootPerson, false)).thenReturn(List.of(rootRel, spouse1Rel, spouse2Rel));
        when(personService.getPeopleInTree(spouse1, true, true, true)).thenReturn(List.of(sp1Origin, sp1Ancestor));
        when(personService.getPeopleInTree(spouse2, true, true, true)).thenReturn(List.of(sp2Origin, sp2Ancestor));

        List<List<Relationship>> result = familyTreeHelper.getRelationshipsWithNotInLawPriority(rootPerson, true);

        // sharedAncestor must appear exactly once
        long count = result.stream()
                .filter(group -> group.getFirst().person().getId().equals(4))
                .count();
        assertThat(count).isEqualTo(1);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static EnrichedPerson mockPerson(int id) {
        EnrichedPerson p = mock(EnrichedPerson.class);
        when(p.getId()).thenReturn(id);
        return p;
    }

    private static Relationship rel(EnrichedPerson person, int asc, int desc, boolean isInLaw) {
        return new Relationship(person, asc, desc, isInLaw, false, false, null, null, null, null);
    }

    private static Set<Integer> ids(List<List<Relationship>> result) {
        return result.stream()
                .map(group -> group.getFirst().person().getId())
                .collect(java.util.stream.Collectors.toSet());
    }
}
