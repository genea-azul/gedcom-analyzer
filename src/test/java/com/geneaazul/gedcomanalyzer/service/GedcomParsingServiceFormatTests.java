package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
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
        return new Relationship(person, asc, desc, false, false, null, null, null, null);
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

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

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

        gedcomParsingService.format(original, rels, 0, 10, 10);

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

        gedcomParsingService.format(original, rels, 0, 10, 10);

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

        Gedcom result = gedcomParsingService.format(original, allPeople, 0, 10, 10);

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

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

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

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

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

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

        assertThat(result.getPeople()).hasSize(1);
        assertThat(result.getFamilies()).isEmpty();
    }

    @Test
    void format_familyExcludedWhenSingleSpouseAndNoChildren() throws Exception {
        // Include I2 only (I1 and I3 excluded).
        // F1: husb=[], wife=[I2], chil=[] → 1 spouse + no children → excluded
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

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

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

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

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

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

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

        assertThat(parentFamilyIds(result.getPerson("I3"))).containsExactly("F1");
        assertThat(parentFamilyIds(result.getPerson("I2"))).isEmpty();
    }

    @Test
    void format_personFamilyRefs_emptyWhenAllFamiliesExcluded() throws Exception {
        // Include I4 only — F2 excluded (no spouses remain).
        // I4.parentFamilyRefs must be empty (F2 excluded).
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(4))));

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

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

        Gedcom result = gedcomParsingService.format(original, rels, 4, 2, 2);

        Set<String> resultIds = result.getPeople().stream()
                .map(Person::getId)
                .collect(Collectors.toSet());
        assertThat(resultIds).containsExactlyInAnyOrder("I2", "I3", "I4");
    }

    @Test
    void format_noTrimming_whenRelationshipsCountBelowThreshold() throws Exception {
        // 3 persons, threshold=5 → trimGedcom=false, all included regardless of distances.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(rel(person(2), 0, 0)),
                List.of(rel(person(3), 0, 0)),
                List.of(rel(person(4), 0, 0)));

        Gedcom result = gedcomParsingService.format(original, rels, 5, 0, 0);

        assertThat(result.getPeople()).hasSize(3);
    }

    @Test
    void format_noTrimming_whenThresholdIsZero() throws Exception {
        // threshold=0 → trimGedcom=false, all included regardless of distances.
        Gedcom original = freshGedcom();
        List<List<Relationship>> allPeople = enrichedGedcom.getPeople().stream()
                .map(p -> rel(p, 100, 100))
                .map(List::of)
                .toList();

        Gedcom result = gedcomParsingService.format(original, allPeople, 0, 1, 1);

        assertThat(result.getPeople()).hasSameSizeAs(original.getPeople());
    }

    // ── returned Gedcom integrity ─────────────────────────────────────────────

    @Test
    void format_returnedGedcomHasCorrectHeader() throws Exception {
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(List.of(Relationship.empty(person(2))));

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

        assertThat(result.getHeader().getCharacterSet().getValue()).isEqualTo("UTF-8");
        assertThat(result.getHeader().getGedcomVersion().getVersion()).isEqualTo("5.5.1");
        assertThat(result.getHeader().getLanguage()).isEqualTo("Spanish");
    }

    @Test
    void format_returnedGedcomHasIndexesPopulated() throws Exception {
        // Verifies createIndexes() was called: getFamily/getPerson lookups by ID work.
        Gedcom original = freshGedcom();
        List<List<Relationship>> rels = List.of(
                List.of(Relationship.empty(person(2))),
                List.of(Relationship.empty(person(3))));

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);

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
        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);
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

        Gedcom result = gedcomParsingService.format(original, rels, 0, 10, 10);
        gedcomParsingService.write(result, outputPath);

        Gedcom reparsed = gedcomParsingService.parseGedcom(outputPath);
        Person i2 = reparsed.getPerson("I2");
        assertThat(i2).isNotNull();
        assertThat(i2.getNames()).isNotEmpty();
        assertThat(i2.getNames().getFirst().getValue()).contains("Test Mother");
    }
}
