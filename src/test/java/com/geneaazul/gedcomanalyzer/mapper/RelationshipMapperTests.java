package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.dto.AdoptionType;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.RelationshipDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class RelationshipMapperTests {

    @Autowired
    private RelationshipMapper relationshipMapper;

    @Test
    public void testFormatInSpanish_Self() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.SELF)
                .generation(0)
                .grade(-1)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(null)
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                null,
                "★",
                " ",
                "persona principal",
                false));
    }

    @Test
    public void testFormatInSpanish_Spouse() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.SPOUSE)
                .generation(0)
                .grade(-1)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.SPOUSE, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                null,
                "★",
                "◁",
                "ex-pareja",
                false));
    }

    @Test
    public void testFormatInSpanish_Parent() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(8)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de heptabuela",
                false));
    }

    @Test
    public void testFormatInSpanish_Child() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.CHILD)
                .generation(8)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de heptanieta",
                false));
    }

    @Test
    public void testFormatInSpanish_Sibling() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.SIBLING)
                .generation(0)
                .grade(0)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-hermana",
                false));
    }

    @Test
    public void testFormatInSpanish_Cousin() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.COUSIN)
                .generation(0)
                .grade(4)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-prima 4ta",
                false));
    }

    @Test
    public void testFormatInSpanishPibling_1_1() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PIBLING)
                .generation(1)
                .grade(1)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-tía",
                false));
    }

    @Test
    public void testFormatInSpanishNibling_1_1() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.NIBLING)
                .generation(1)
                .grade(1)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-sobrina",
                false));
    }

    @Test
    public void testFormatInSpanishPibling_1_4() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PIBLING)
                .generation(1)
                .grade(4)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-tía 4ta  (ex-pareja de medio-prima 3ra de padre/madre adoptivo/a)",
                false));
    }

    @Test
    public void testFormatInSpanishNibling_1_4() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.NIBLING)
                .generation(1)
                .grade(4)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-sobrina 4ta  (ex-pareja de hija adoptiva de medio-primo/a 3ro/a)",
                false));
    }

    @Test
    public void testFormatInSpanishPibling_8_1() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PIBLING)
                .generation(8)
                .grade(1)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-tía-heptabuela  (ex-pareja de medio-hermana de heptabuelo/a)",
                false));
    }

    @Test
    public void testFormatInSpanishNibling_8_1() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.NIBLING)
                .generation(8)
                .grade(1)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-sobrina-heptanieta  (ex-pareja de heptanieta de medio-hermano/a)",
                false));
    }

    @Test
    public void testFormatInSpanishPibling_8_4() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PIBLING)
                .generation(8)
                .grade(4)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-tía-heptabuela 4ta  (ex-pareja de medio-prima 3ra de heptabuelo/a)",
                false));
    }

    @Test
    public void testFormatInSpanishNibling_8_4() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.NIBLING)
                .generation(8)
                .grade(4)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "ex-pareja de medio-sobrina-heptanieta 4ta  (ex-pareja de heptanieta de medio-primo/a 3ro/a)",
                false));
    }

    @Test
    public void testFormatInSpanish_Relative() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.FAMILY)
                .generation(0)
                .grade(0)
                .isInLaw(true)
                .isHalf(true)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                "✝",
                "~1823",
                "Argentina",
                "ADOPTIVE",
                "★",
                "↔",
                "familiar",
                false));
    }

}
