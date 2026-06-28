package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.AlivePersonFilter;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link GedcomParsingService#format}.
 *
 * Test data — test-gedcom-001.ged key nodes:
 *   I1 (Father): in F1 as HUSB, in F2 as HUSB — no explicit FAMS tags
 *   I2 (Mother): FAMS F1
 *   I3 (Daughter): FAMC F1, FAMS F3
 *   I4 (Other son): FAMC F2
 *   I5 (Grandson): no FAMS/FAMC
 *   F1: HUSB I1, WIFE I2, CHIL I3
 *   F2: HUSB I1, CHIL I4
 *   F3: WIFE I3, CHIL I5
 */
@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
class GedcomParsingServiceFormatTests {

    @Autowired
    private GedcomParsingService gedcomParsingService;
    @Autowired
    private GedcomAnalyzerProperties properties;
    @Autowired
    private GedcomHolder gedcomHolder;

    @TempDir
    Path tempDir;

    private EnrichedGedcom enrichedGedcom;

    @BeforeEach
    void setUp() {
        enrichedGedcom = gedcomHolder.getGedcom();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Gedcom freshGedcom() throws Exception {
        return gedcomParsingService.parseGedcom(properties.getGedcomStorageLocalPath());
    }

    private EnrichedPerson person(int id) {
        return Objects.requireNonNull(enrichedGedcom.getPersonById(id), "person " + id + " not found");
    }

    private static Relationship rel(EnrichedPerson person, int asc, int desc) {
        return new Relationship(person, asc, desc, false, false, false, null, null, null, null);
    }

    private static Relationship inLaw(EnrichedPerson person, int asc, int desc) {
        return new Relationship(person, asc, desc, true, false, false, null, null, null, null);
    }

    private static List<String> husbandIds(Family f) {
        return f.getHusbandRefs().stream().map(SpouseRef::getRef).toList();
    }

    private static List<String> wifeIds(Family f) {
        return f.getWifeRefs().stream().map(SpouseRef::getRef).toList();
    }

    private static List<String> childIds(Family f) {
        return f.getChildRefs().stream().map(ChildRef::getRef).toList();
    }

    private static List<String> spouseFamilyIds(Person p) {
        return p.getSpouseFamilyRefs().stream().map(SpouseFamilyRef::getRef).toList();
    }

    private static List<String> parentFamilyIds(Person p) {
        return p.getParentFamilyRefs().stream().map(ParentFamilyRef::getRef).toList();
    }

    // ── mutation guard ────────────────────────────────────────────────────────

    @Test
    void format_doesNotMutateInputFamilyChildRefs() throws Exception {
        Gedcom original = freshGedcom();

        // F1 has CHIL=I3. Include I1+I2 only → result F1 copy should have empty childRefs.
        // The original Family object must NOT be modified.
        Family originalF1 = original.getFamily("F1");
        List<String> snapshotChildRefs = childIds(originalF1);
        assertThat(snapshotChildRefs).containsExactly("I3");

        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        // Input object unchanged
        assertThat(childIds(originalF1)).isEqualTo(snapshotChildRefs);

        // Result copy has empty childRefs
        assertThat(result.getFamily("F1").getChildRefs()).isEmpty();
    }

    @Test
    void format_doesNotMutateInputFamilySpouseRefs() throws Exception {
        Gedcom original = freshGedcom();

        // F1 has HUSB=I1, WIFE=I2. Include I3 only → result F1 should have no husband/wife refs.
        Family originalF1 = original.getFamily("F1");
        List<String> snapshotHusbands = husbandIds(originalF1);
        List<String> snapshotWives   = wifeIds(originalF1);
        assertThat(snapshotHusbands).containsExactly("I1");
        assertThat(snapshotWives).containsExactly("I2");

        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(3))));

        gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(husbandIds(originalF1)).isEqualTo(snapshotHusbands);
        assertThat(wifeIds(originalF1)).isEqualTo(snapshotWives);
    }

    @Test
    void format_doesNotMutateInputPersonFamilyRefs() throws Exception {
        Gedcom original = freshGedcom();

        // I3 has FAMC=F1 and FAMS=F3. Include I3 only → F1 and F3 both excluded.
        // The original Person object must NOT have its refs cleared.
        Person originalI3 = original.getPerson("I3");
        List<String> snapshotFamc = parentFamilyIds(originalI3);
        List<String> snapshotFams = spouseFamilyIds(originalI3);
        assertThat(snapshotFamc).containsExactly("F1");
        assertThat(snapshotFams).containsExactly("F3");

        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(3))));

        gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(parentFamilyIds(originalI3)).isEqualTo(snapshotFamc);
        assertThat(spouseFamilyIds(originalI3)).isEqualTo(snapshotFams);
    }

    // ── person / family counts ────────────────────────────────────────────────

    @Test
    void format_noThreshold_includesAllPeopleAndFamilies() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> allPeople = enrichedGedcom.getPeople().stream()
                .map(Relationship::empty)
                .map(List::of)
                .toList();

        Gedcom result = gedcomParsingService.format(original, allPeople, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPeople()).hasSameSizeAs(original.getPeople());
        assertThat(result.getFamilies()).hasSameSizeAs(original.getFamilies());
    }

    @Test
    void format_subsetOfPeople_onlyMatchingFamiliesSurvive() throws Exception {
        // Include I2, I3, I4.
        // F1 (HUSB=I1, WIFE=I2, CHIL=I3): husb=[], wife=[I2], chil=[I3] → kept (wife + child present)
        // F2 (HUSB=I1, CHIL=I4):           husb=[], wife=[],  chil=[I4] → excluded (no spouses)
        // F3 (WIFE=I3, CHIL=I5):           husb=[], wife=[I3], chil=[] → excluded (1 spouse, no child)
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))),
                List.of(Relationship.empty(person(4))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPeople()).hasSize(3);
        assertThat(result.getFamilies()).hasSize(1);
        assertThat(result.getFamily("F1")).isNotNull();
        assertThat(result.getFamily("F2")).isNull();
        assertThat(result.getFamily("F3")).isNull();
    }

    // ── family ref filtering ──────────────────────────────────────────────────

    @Test
    void format_familyRefs_filteredToIncludedPersonsOnly() throws Exception {
        // Include I2 and I3 — I1 and I5 are excluded.
        // F1 result copy: husb=[], wife=[I2], chil=[I3]
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        Family f1 = result.getFamily("F1");
        assertThat(f1).isNotNull();
        assertThat(husbandIds(f1)).isEmpty();
        assertThat(wifeIds(f1)).containsExactly("I2");
        assertThat(childIds(f1)).containsExactly("I3");
    }

    @Test
    void format_familyExcludedWhenNoSpousesRemainAfterFiltering() throws Exception {
        // Include I3 only (I1, I2, I5 excluded).
        // F1: husb=[], wife=[], chil=[I3] → excluded (no spouses at all)
        // F3: husb=[], wife=[I3], chil=[] → excluded (1 spouse, no child)
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPeople()).hasSize(1);
        assertThat(result.getFamilies()).isEmpty();
    }

    @Test
    void format_familyExcludedWhenSingleSpouseAndNoChildren() throws Exception {
        // Include I2 only (I1 and I3 excluded).
        // F1: husb=[], wife=[I2], chil=[] → 1 spouse + no children → excluded
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPeople()).hasSize(1);
        assertThat(result.getFamilies()).isEmpty();
    }

    @Test
    void format_familyWithBothSpousesSurvivedEvenWithNoChildren() throws Exception {
        // Include I1 and I2 (I3 excluded).
        // F1: husb=[I1], wife=[I2], chil=[] → couple with no children → KEPT
        // F2: husb=[I1], wife=[],  chil=[] → 1 spouse, no children → excluded
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getFamilies()).hasSize(1);
        Family f1 = result.getFamily("F1");
        assertThat(f1).isNotNull();
        assertThat(husbandIds(f1)).containsExactly("I1");
        assertThat(wifeIds(f1)).containsExactly("I2");
        assertThat(childIds(f1)).isEmpty();
    }

    // ── person ref filtering ──────────────────────────────────────────────────

    @Test
    void format_personSpouseFamilyRefs_filteredToSurvivingFamilies() throws Exception {
        // Include I2 and I3.
        // F1 survives → I2.spouseFamilyRefs = [F1]
        // F3 excluded  → I3.spouseFamilyRefs = []
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(spouseFamilyIds(result.getPerson("I2"))).containsExactly("F1");
        assertThat(spouseFamilyIds(result.getPerson("I3"))).isEmpty();
    }

    @Test
    void format_personParentFamilyRefs_filteredToSurvivingFamilies() throws Exception {
        // Include I2 and I3.
        // F1 survives → I3.parentFamilyRefs = [F1]
        // I2 has no FAMC tag → I2.parentFamilyRefs = []
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(parentFamilyIds(result.getPerson("I3"))).containsExactly("F1");
        assertThat(parentFamilyIds(result.getPerson("I2"))).isEmpty();
    }

    @Test
    void format_personFamilyRefs_emptyWhenAllFamiliesExcluded() throws Exception {
        // Include I4 only — F2 excluded (no spouses remain).
        // I4.parentFamilyRefs must be empty (F2 excluded).
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(4))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPeople()).hasSize(1);
        assertThat(result.getFamilies()).isEmpty();
        assertThat(parentFamilyIds(result.getPerson("I4"))).isEmpty();
    }

    // ── trimming by distance threshold ────────────────────────────────────────

    @Test
    void format_trimming_excludesPeopleOutsideDistanceThresholds() throws Exception {
        // Relationships: 3 close (asc=0, desc=0) + 2 far (asc=10, desc=10).
        // threshold=4 → trimGedcom=true (5 > 4).
        // maxAsc=2, maxDesc=2: close (0,0) pass via "0 < 2"; far (10,10) fail all conditions.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 0, 0)),
                List.of(rel(person(3), 0, 0)),
                List.of(rel(person(4), 0, 0)),
                List.of(rel(person(6), 10, 10)),
                List.of(rel(person(7), 10, 10)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 4, 2, 2, null, true, true);

        Set<String> resultIds = result.getPeople().stream()
                .map(Person::getId)
                .collect(Collectors.toSet());
        assertThat(resultIds).containsExactlyInAnyOrder("I2", "I3", "I4");
    }

    @Test
    void format_trimming_includesPersonAtExactBoundary() throws Exception {
        // (asc=2, desc=2) with maxAsc=2, maxDesc=2: exactly on the boundary → included.
        // (asc=2, desc=3) with maxAsc=2, maxDesc=2: one step past the boundary → excluded.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 2, 2)),
                List.of(rel(person(3), 2, 3)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 1, 2, 2, null, true, true);

        Set<String> resultIds = result.getPeople().stream().map(Person::getId).collect(Collectors.toSet());
        assertThat(resultIds).containsExactly("I2");
    }

    @Test
    void format_trimming_beyondAscThresholdIsAlwaysExcluded() throws Exception {
        // asc > maxAsc is excluded regardless of desc — ancestors beyond the limit are dropped.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 2, 0)),   // asc == maxAsc → included
                List.of(rel(person(3), 3, 0)),   // asc > maxAsc, desc=0 → excluded
                List.of(rel(person(4), 3, 1)));  // asc > maxAsc, desc=1 → excluded

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 2, 2, 2, null, true, true);

        Set<String> resultIds = result.getPeople().stream().map(Person::getId).collect(Collectors.toSet());
        assertThat(resultIds).containsExactly("I2");
    }

    @Test
    void format_trimming_descIsCappedAtAllAscLevels() throws Exception {
        // desc > maxDesc is excluded even when asc is well within the threshold.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 1, 2)),   // asc=1 < maxAsc=2, desc=2 == maxDesc → included
                List.of(rel(person(3), 1, 3)));   // asc=1 < maxAsc=2, desc=3 > maxDesc=2 → excluded

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 1, 2, 2, null, true, true);

        Set<String> resultIds = result.getPeople().stream().map(Person::getId).collect(Collectors.toSet());
        assertThat(resultIds).containsExactly("I2");
    }

    @Test
    void format_noTrimming_whenRelationshipsCountBelowThreshold() throws Exception {
        // 3 persons, threshold=5 → trimGedcom=false, all included regardless of distances.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 0, 0)),
                List.of(rel(person(3), 0, 0)),
                List.of(rel(person(4), 0, 0)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 5, 0, 0, null, true, true);

        assertThat(result.getPeople()).hasSize(3);
    }

    @Test
    void format_noTrimming_whenThresholdIsNull() throws Exception {
        // threshold=null → trimGedcom=false, all included regardless of distances.
        Gedcom original = freshGedcom();
        List<List<Relationship>> allPeople = enrichedGedcom.getPeople().stream()
                .map(p -> rel(p, 100, 100))
                .map(List::of)
                .toList();

        Gedcom result = gedcomParsingService.format(original, allPeople, AlivePersonFilter.ALLOW, false, false, null, null, 1, 1, null, true, true);

        assertThat(result.getPeople()).hasSameSizeAs(original.getPeople());
    }

    @Test
    void format_alwaysTrimming_whenThresholdIsZero() throws Exception {
        // threshold=0 → always trim regardless of tree size; only persons within distance survive.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 0, 0)),   // desc=0 ≤ maxDesc=0 → included
                List.of(rel(person(3), 0, 1)));   // desc=1 > maxDesc=0 → excluded

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 0, 5, 0, null, true, true);

        Set<String> resultIds = result.getPeople().stream().map(Person::getId).collect(Collectors.toSet());
        assertThat(resultIds).containsExactly("I2");
    }

    // ── trimming: ancestor overflow (clause 2) ───────────────────────────────

    @Test
    void format_trimming_ancestorOverflow_reAdmitsAncestorsBeyondAscThreshold() throws Exception {
        // maxAsc=1, maxDesc=2, maxDescForAncestors=1.
        // I2 (asc=1,desc=0): clause 1 → included.
        // I3 (asc=2,desc=0): clause 2 (asc>1, desc=0≤1) → included via overflow.
        // I4 (asc=2,desc=1): clause 2 (asc>1, desc=1≤1) → included via overflow.
        // I5 (asc=2,desc=2): clause 1 fails (asc>1), clause 2 fails (desc=2>1) → excluded.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 1, 0)),
                List.of(rel(person(3), 2, 0)),
                List.of(rel(person(4), 2, 1)),
                List.of(rel(person(5), 2, 2)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 3, 1, 2, 1, true, true);

        Set<String> resultIds = result.getPeople().stream().map(Person::getId).collect(Collectors.toSet());
        assertThat(resultIds).containsExactlyInAnyOrder("I2", "I3", "I4");
    }

    @Test
    void format_trimming_ancestorOverflow_disabledByNull() throws Exception {
        // Same setup as above but maxDescForAncestors=null → clause 2 disabled.
        // I3 (asc=2,desc=0) and I4 (asc=2,desc=1): asc>maxAsc=1, no overflow → excluded.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 1, 0)),
                List.of(rel(person(3), 2, 0)),
                List.of(rel(person(4), 2, 1)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 2, 1, 2, null, true, true);

        Set<String> resultIds = result.getPeople().stream().map(Person::getId).collect(Collectors.toSet());
        assertThat(resultIds).containsExactly("I2");
    }

    // ── trimming: spouse flags ────────────────────────────────────────────────

    @Test
    void format_trimming_spouseFlagA_excludesInLawsAtExactDescBoundary() throws Exception {
        // maxAsc=2, maxDesc=2, includeInLawsAtMaxDescDepth=false.
        // I2 (asc=1,desc=1, not in-law): included.
        // I3 (asc=1,desc=2, not in-law): included (desc==maxDesc but not in-law).
        // I4 (asc=1,desc=1, in-law): included (desc<maxDesc, flag doesn't fire).
        // I5 (asc=1,desc=2, in-law): EXCLUDED (desc==maxDesc AND in-law AND flag=false).
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 1, 1)),
                List.of(rel(person(3), 1, 2)),
                List.of(inLaw(person(4), 1, 1)),
                List.of(inLaw(person(5), 1, 2)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 3, 2, 2, null, false, true);

        Set<String> resultIds = result.getPeople().stream().map(Person::getId).collect(Collectors.toSet());
        assertThat(resultIds).containsExactlyInAnyOrder("I2", "I3", "I4");
    }

    @Test
    void format_trimming_spouseFlagB_excludesInLawsAtExactAscBoundary() throws Exception {
        // maxAsc=2, maxDesc=2, includeInLawsAtMaxAscDepth=false.
        // I2 (asc=1,desc=1, in-law): included (asc<maxAsc, flag doesn't fire).
        // I3 (asc=2,desc=1, not in-law): included (asc==maxAsc but not in-law).
        // I4 (asc=2,desc=1, in-law): EXCLUDED (asc==maxAsc AND in-law AND flag=false).
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(inLaw(person(2), 1, 1)),
                List.of(rel(person(3), 2, 1)),
                List.of(inLaw(person(4), 2, 1)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, 2, 2, 2, null, true, false);

        Set<String> resultIds = result.getPeople().stream().map(Person::getId).collect(Collectors.toSet());
        assertThat(resultIds).containsExactlyInAnyOrder("I2", "I3");
    }

    // ── returned Gedcom integrity ─────────────────────────────────────────────

    @Test
    void format_returnedGedcomHasCorrectHeader() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getHeader().getCharacterSet().getValue()).isEqualTo("UTF-8");
        assertThat(result.getHeader().getGedcomVersion().getVersion()).isEqualTo("5.5.1");
        assertThat(result.getHeader().getLanguage()).isEqualTo("Spanish");
    }

    @Test
    void format_rootPersonId_writesRootExtensionToHeader() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, 2, null, 10, 10, null, true, true);

        @SuppressWarnings("unchecked")
        List<org.folg.gedcom.model.GedcomTag> moreTags =
                (List<org.folg.gedcom.model.GedcomTag>) result.getHeader()
                        .getExtension(org.folg.gedcom.parser.ModelParser.MORE_TAGS_EXTENSION_KEY);
        assertThat(moreTags).isNotNull().hasSize(1);
        assertThat(moreTags.getFirst().getTag()).isEqualTo("_ROOT");
        assertThat(moreTags.getFirst().getRef()).isEqualTo("I2");
    }

    @Test
    void format_rootPersonId_null_doesNotWriteRootExtension() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getHeader().getExtension(org.folg.gedcom.parser.ModelParser.MORE_TAGS_EXTENSION_KEY)).isNull();
    }

    @Test
    void format_returnedGedcomHasIndexesPopulated() throws Exception {
        // Verifies createIndexes() was called: getFamily/getPerson lookups by ID work.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getFamily("F1")).isNotNull();
        assertThat(result.getPerson("I2")).isNotNull();
        assertThat(result.getPerson("I3")).isNotNull();
        assertThat(result.getPerson("I1")).isNull();  // excluded
    }

    // ── I/O correctness ───────────────────────────────────────────────────────

    @Test
    void format_outputFileIsWrittenAndParseable() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Path outputPath = tempDir.resolve("output.ged");
        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);
        gedcomParsingService.write(result, outputPath);

        assertThat(outputPath).exists();
        Gedcom reparsed = gedcomParsingService.parseGedcom(outputPath);
        assertThat(reparsed.getPeople()).hasSameSizeAs(result.getPeople());
        assertThat(reparsed.getFamilies()).hasSameSizeAs(result.getFamilies());
    }

    @Test
    void write_preservesPersonData() throws Exception {
        // Verifies that person attributes survive the write/parse round-trip.
        Gedcom original = freshGedcom();
        Path outputPath = tempDir.resolve("output.ged");
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);
        gedcomParsingService.write(result, outputPath);

        Gedcom reparsed = gedcomParsingService.parseGedcom(outputPath);
        Person i2 = reparsed.getPerson("I2");
        assertThat(i2).isNotNull();
        assertThat(i2.getNames()).isNotEmpty();
        assertThat(i2.getNames().getFirst().getValue()).contains("Test Mother");
    }

    // ── directLineageOnly ─────────────────────────────────────────────────────
    //
    // isDirect() = distanceToAncestorRootPerson==0 (descendant) OR distanceToAncestorThisPerson==0 (direct ancestor).
    // Collateral relatives (siblings, uncles, cousins) have both distances > 0 → excluded.

    @Test
    void format_directLineageOnly_excludesCollateralRelatives() throws Exception {
        // I2 as "parent" (asc=1, desc=0) → isDirect → kept
        // I3 as "sibling" (asc=1, desc=1) → not direct → excluded
        // I4 as "uncle"   (asc=2, desc=1) → not direct → excluded
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 1, 0)),
                List.of(rel(person(3), 1, 1)),
                List.of(rel(person(4), 2, 1)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, true, null, null, 10, 10, null, true, true);

        assertThat(result.getPerson("I2")).isNotNull();
        assertThat(result.getPerson("I3")).isNull();
        assertThat(result.getPerson("I4")).isNull();
    }

    @Test
    void format_directLineageOnly_includesDirectAncestorsAndDescendants() throws Exception {
        // I2 as "parent" (asc=1, desc=0)      → isDirect → kept
        // I3 as "grandparent" (asc=2, desc=0) → isDirect → kept
        // I4 as "child" (asc=0, desc=1)       → isDirect → kept
        // I5 as "grandchild" (asc=0, desc=2)  → isDirect → kept
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 1, 0)),
                List.of(rel(person(3), 2, 0)),
                List.of(rel(person(4), 0, 1)),
                List.of(rel(person(5), 0, 2)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, true, null, null, 10, 10, null, true, true);

        assertThat(result.getPerson("I2")).isNotNull();
        assertThat(result.getPerson("I3")).isNotNull();
        assertThat(result.getPerson("I4")).isNotNull();
        assertThat(result.getPerson("I5")).isNotNull();
    }

    @Test
    void format_directLineageOnly_includesSpousesOfDirectAncestors() throws Exception {
        // A spouse of an ancestor has distanceToAncestorThisPerson==0 and isInLaw==true → isDirect → kept.
        Gedcom original = freshGedcom();
        Relationship spouseOfParent = new Relationship(person(2), 1, 0, true, false, false, null, null, null, null);
        List<List<Relationship>> rels = List.of(List.of(spouseOfParent));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, true, null, null, 10, 10, null, true, true);

        assertThat(result.getPerson("I2")).isNotNull();
    }

    @Test
    void format_directLineageOnly_false_includesCollateralRelatives() throws Exception {
        // With directLineageOnly=false (default), collateral relatives are not filtered out.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(3), 1, 1)),
                List.of(rel(person(4), 2, 1)));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPerson("I3")).isNotNull();
        assertThat(result.getPerson("I4")).isNotNull();
    }

    // ── AlivePersonFilter.SKIP ────────────────────────────────────────────────
    //
    // I1–I22 are alive; I23 (Test Deceased) has DEAT+BURI and is not alive.

    @Test
    void format_skip_excludesAllAlivePersonsAndTheirFamilies() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.SKIP, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPeople()).isEmpty();
        assertThat(result.getFamilies()).isEmpty();
    }

    // ── AlivePersonFilter.SHOW_SURNAME_ONLY ──────────────────────────────────

    @Test
    void format_showSurnameOnly_includesAlivePersonsInResult() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.SHOW_SURNAME_ONLY, false, false, null, null, 10, 10, null, true, true);

        // All 3 alive persons are still included.
        assertThat(result.getPeople()).hasSize(3);
    }

    @Test
    void format_showSurnameOnly_personNameContainsOnlySurname() throws Exception {
        // I1: NAME "Test Father /Family1/", GIVN "Test Father", SURN "Family1"
        // → after SHOW_SURNAME_ONLY: value is "<privado> /Family1/", given is "<privado>", surname retained, prefix/suffix stripped.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.SHOW_SURNAME_ONLY, false, false, null, null, 10, 10, null, true, true);

        Person i1 = result.getPerson("I1");
        assertThat(i1.getNames()).isNotEmpty();
        Name name = i1.getNames().getFirst();
        assertThat(name.getValue()).isEqualTo("<privado> /Family1/");
        assertThat(name.getSurname()).isEqualTo("Family1");
        assertThat(name.getGiven()).isEqualTo("<privado>");
        assertThat(name.getNickname()).isNull();
        assertThat(name.getPrefix()).isNull();
        assertThat(name.getSuffix()).isNull();
    }

    @Test
    void format_showSurnameOnly_noEventsOnAlivePersons() throws Exception {
        // I2 has SEX, BIRT, RESI, EVEN — all stripped because person is alive.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.SHOW_SURNAME_ONLY, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPerson("I2").getEventsFacts()).isEmpty();
    }

    @Test
    void format_showSurnameOnly_familyStructurePreserved() throws Exception {
        // Include I1+I2+I3 → F1 (husb=I1, wife=I2, chil=I3) survives.
        // F2 (husb=I1, chil=I4 excluded) and F3 (wife=I3, chil=I5 excluded) are dropped.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.SHOW_SURNAME_ONLY, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getFamilies()).hasSize(1);
        Family f1 = result.getFamily("F1");
        assertThat(husbandIds(f1)).containsExactly("I1");
        assertThat(wifeIds(f1)).containsExactly("I2");
        assertThat(childIds(f1)).containsExactly("I3");
    }

    @Test
    void format_showSurnameOnly_familyEventsStrippedWhenBothSpousesAlive() throws Exception {
        // F1 has husb=I1 (alive) and wife=I2 (alive) → events must be stripped.
        // Original F1 has MARR and DIV events.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.SHOW_SURNAME_ONLY, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getFamily("F1").getEventsFacts()).isEmpty();
    }

    // ── displayOnlyBasic ─────────────────────────────────────────────────────

    @Test
    void format_displayOnlyBasic_stripsNonEssentialPersonEvents() throws Exception {
        // I2 has: SEX, BIRT, RESI (with email sub-tag), EVEN (Personalidad destacada/Comment)
        // Basic mode keeps only SEX + BIRT.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        List<String> tags = result.getPerson("I2").getEventsFacts().stream().map(EventFact::getTag).toList();
        assertThat(tags).containsExactlyInAnyOrder("SEX", "BIRT");
    }

    @Test
    void format_displayOnlyBasic_keepsBaptismEvent() throws Exception {
        // I3 has: SEX, BIRT, RESI, BAPM — basic mode keeps SEX + BIRT + BAPM, strips RESI.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        List<String> tags = result.getPerson("I3").getEventsFacts().stream().map(EventFact::getTag).toList();
        assertThat(tags).containsExactlyInAnyOrder("SEX", "BIRT", "BAPM");
    }

    @Test
    void format_displayOnlyBasic_preservesNotesOnPersonBirthEvent() throws Exception {
        // I1's BIRT has: DATE, PLAC, NOTE "Missing spouse references on purpose."
        // Basic mode preserves the note along with date and place.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        EventFact birt = result.getPerson("I1").getEventsFacts().stream()
                .filter(e -> "BIRT".equals(e.getTag())).findFirst().orElseThrow();
        assertThat(birt.getDate()).isEqualTo("01 JAN 1980");
        assertThat(birt.getPlace()).isEqualTo("Azul, Buenos Aires, Argentina");
        assertThat(birt.getNotes()).hasSize(1);
        assertThat(birt.getNotes().getFirst().getValue()).isEqualTo("Missing spouse references on purpose.");
    }

    @Test
    void format_displayOnlyBasic_preservesPersonNameFields() throws Exception {
        // I2's name: given="Test Mother", surname="Family2" — both must survive basic mode.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        Name name = result.getPerson("I2").getNames().getFirst();
        assertThat(name.getGiven()).isEqualTo("Test Mother");
        assertThat(name.getSurname()).isEqualTo("Family2");
        assertThat(name.getValue()).contains("Test Mother");
    }

    @Test
    void format_displayOnlyBasic_keepsMarriageEventWithDateAndPlace() throws Exception {
        // F1 has: MARR DATE "15 JUL 2005" + DIV DATE "20 DEC 2010"
        // Basic mode keeps both events with date and place; notes/sources are stripped.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        Family f1 = result.getFamily("F1");
        assertThat(f1).isNotNull();
        assertThat(f1.getEventsFacts()).hasSize(2);
        EventFact marr = f1.getEventsFacts().getFirst();
        assertThat(marr.getTag()).isEqualTo("MARR");
        assertThat(marr.getDate()).isEqualTo("15 JUL 2005");
        assertThat(marr.getPlace()).startsWith("Catedral Nuestra Señora");
        assertThat(marr.getNotes()).isEmpty();
        assertThat(marr.getSourceCitations()).isEmpty();
    }

    @Test
    void format_displayOnlyBasic_keepsDivorceEventWithDateAndPlace() throws Exception {
        // F1 has: DIV Y DATE "20 DEC 2010", PLAC "Azul, Buenos Aires, Argentina"
        // Basic mode must keep it; notes/sources are stripped.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        Family f1 = result.getFamily("F1");
        assertThat(f1).isNotNull();
        EventFact div = f1.getEventsFacts().stream()
                .filter(e -> "DIV".equals(e.getTag())).findFirst().orElseThrow();
        assertThat(div.getValue()).isEqualTo("Y");
        assertThat(div.getDate()).isEqualTo("20 DEC 2010");
        assertThat(div.getPlace()).isEqualTo("Azul, Buenos Aires, Argentina");
        assertThat(div.getNotes()).isEmpty();
    }

    @Test
    void format_displayOnlyBasic_keepsSeparationEventOnFamily() throws Exception {
        // F3 has: EVEN TYPE="Separation" DATE "20 MAR 2015" — must survive the basic filter.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(3))),
                List.of(Relationship.empty(person(5))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        Family f3 = result.getFamily("F3");
        assertThat(f3).isNotNull();
        assertThat(f3.getEventsFacts()).hasSize(1);
        EventFact sep = f3.getEventsFacts().getFirst();
        assertThat(sep.getTag()).isEqualTo("EVEN");
        assertThat(sep.getType()).isEqualTo("Separation");
        assertThat(sep.getDate()).isEqualTo("20 MAR 2015");
        assertThat(sep.getNotes()).isEmpty();
    }

    @Test
    void format_displayOnlyBasic_keepsChristeningEventAndPreservesItsNote() throws Exception {
        // I23 has CHR DATE "15 MAR 1950" with an inline NOTE — basic mode keeps CHR and preserves the NOTE.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(23))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        EventFact chr = result.getPerson("I23").getEventsFacts().stream()
                .filter(e -> "CHR".equals(e.getTag())).findFirst().orElseThrow();
        assertThat(chr.getDate()).isEqualTo("15 MAR 1950");
        assertThat(chr.getPlace()).isEqualTo("Azul, Buenos Aires, Argentina");
        assertThat(chr.getNotes()).hasSize(1);
        assertThat(chr.getNotes().getFirst().getValue()).isEqualTo("Baptism record reference.");
    }

    @Test
    void format_displayOnlyBasic_keepsDeathAndBurialEvents() throws Exception {
        // I23 has DEAT Y DATE "10 FEB 2020" and BURI DATE "12 FEB 2020" — both must survive.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(23))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        List<String> tags = result.getPerson("I23").getEventsFacts().stream().map(EventFact::getTag).toList();
        assertThat(tags).containsExactlyInAnyOrder("SEX", "CHR", "DEAT", "BURI");

        EventFact deat = result.getPerson("I23").getEventsFacts().stream()
                .filter(e -> "DEAT".equals(e.getTag())).findFirst().orElseThrow();
        assertThat(deat.getValue()).isEqualTo("Y");
        assertThat(deat.getDate()).isEqualTo("10 FEB 2020");

        EventFact buri = result.getPerson("I23").getEventsFacts().stream()
                .filter(e -> "BURI".equals(e.getTag())).findFirst().orElseThrow();
        assertThat(buri.getDate()).isEqualTo("12 FEB 2020");
    }

    @Test
    void format_skip_excludesAlivePersonButIncludesDeceased() throws Exception {
        // I1 is alive → SKIP excludes; I23 has DEAT → not alive → SKIP keeps.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(23))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.SKIP, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPerson("I1")).isNull();
        assertThat(result.getPerson("I23")).isNotNull();
    }

    @Test
    void format_displayOnlyBasic_familyWithNoMarriageEventHasEmptyEvents() throws Exception {
        // F2 has no MARR/MARL/MARB event → after basic filter, family events are empty.
        // F2: HUSB I1, CHIL I4 → kept (spouse + child present).
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(4))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, true, false, null, null, 10, 10, null, true, true);

        assertThat(result.getFamily("F2")).isNotNull();
        assertThat(result.getFamily("F2").getEventsFacts()).isEmpty();
    }

    @Test
    void format_preservesTopLevelNotes() throws Exception {
        // test-gedcom-001.ged has one top-level NOTE @N1@ — it must be present in the result.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 10, 10, null, true, true);

        assertThat(result.getNotes()).hasSize(1);
        Note note = result.getNotes().getFirst();
        assertThat(note.getId()).isEqualTo("N1");
        assertThat(note.getValue()).isEqualTo("Test top-level note.");
    }

    @Test
    void format_displayOnlyBasic_and_skip_combined_emptyResultWhenAllAlive() throws Exception {
        // The typical web sub-gedcom combination: SKIP + basic.
        // I1, I2, I3 are alive → SKIP excludes them → result is empty.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(1))),
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.SKIP, true, false, null, null, 10, 10, null, true, true);

        assertThat(result.getPeople()).isEmpty();
        assertThat(result.getFamilies()).isEmpty();
    }

    @Test
    void format_displayOnlyBasic_showSurnameOnly_combinedStripsEventsAndGivenName() throws Exception {
        // SHOW_SURNAME_ONLY + displayOnlyBasic: alive persons get surname-only names AND no events.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, AlivePersonFilter.SHOW_SURNAME_ONLY, true, false, null, null, 10, 10, null, true, true);

        Person i2 = result.getPerson("I2");
        assertThat(i2.getNames()).isNotEmpty();
        assertThat(i2.getNames().getFirst().getValue()).isEqualTo("<privado> /Family2/");
        assertThat(i2.getNames().getFirst().getSurname()).isEqualTo("Family2");
        assertThat(i2.getNames().getFirst().getGiven()).isEqualTo("<privado>");
        assertThat(i2.getEventsFacts()).isEmpty();
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    void format_validation_negativeMaxAscDepth_throws() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));
        assertThatThrownBy(() -> gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, -1, 0, null, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxAscDepth");
    }

    @Test
    void format_validation_negativeMaxDescDepth_throws() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));
        assertThatThrownBy(() -> gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 0, -1, null, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDescDepth");
    }

    @Test
    void format_validation_negativeTrimTriggerSize_throws() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));
        assertThatThrownBy(() -> gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, -1, 0, 0, null, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trimTriggerSize");
    }

    @Test
    void format_validation_negativeDistantAncestorDescLimit_throws() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));
        assertThatThrownBy(() -> gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 0, 2, -1, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distantAncestorDescLimit");
    }

    @Test
    void format_validation_distantAncestorDescLimitEqualToMaxDescDepth_throws() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));
        assertThatThrownBy(() -> gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 0, 2, 2, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distantAncestorDescLimit");
    }

    @Test
    void format_validation_distantAncestorDescLimitGreaterThanMaxDescDepth_throws() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));
        assertThatThrownBy(() -> gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 0, 2, 3, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distantAncestorDescLimit");
    }

    @Test
    void format_validation_distantAncestorDescLimitWithZeroMaxAscDepth_throws() throws Exception {
        // maxAscDepth=0 with distantAncestorDescLimit set: every ancestor has asc>0, so clause 2
        // would fire for all of them and maxAscDepth=0 becomes meaningless.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));
        assertThatThrownBy(() -> gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 0, 2, 0, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxAscDepth");
    }

    @Test
    void format_validation_zeroMaxAscDepth_withoutDistantAncestorDescLimit_doesNotThrow() throws Exception {
        // maxAscDepth=0 alone is valid (no ancestors in the main range).
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));
        gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 0, 2, null, true, true);
    }

    @Test
    void format_validation_validDistantAncestorDescLimit_doesNotThrow() throws Exception {
        // maxAscDepth=1 > 0, distantAncestorDescLimit=0 < maxDescDepth=2 → valid combination.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(1))));
        gedcomParsingService.format(original, rels, AlivePersonFilter.ALLOW, false, false, null, null, 1, 2, 0, true, true);
    }
}
