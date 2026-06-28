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
                .isSeparated(false)
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
                null,
                "★",
                " ",
                "persona principal",
                false));
    }

    @Test
    public void testFormatInSpanish_Spouse_Sep() {
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
                null,
                "★",
                "◁",
                "ex-pareja",
                false));
    }

    @Test
    public void testFormatInSpanish_Parent_1() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(1)
                .grade(0)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.FATHER))
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
                null,
                "★",
                "←",
                "padre",
                false));
    }

    @Test
    public void testFormatInSpanish_Parent_1_InLaw_Sep() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(1)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER))
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
                null,
                "★",
                "→",
                "ex-pareja de madre",
                false));
    }

    @Test
    public void testFormatInSpanish_Parent_1_InLaw_Adopt_Sep() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(1)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER))
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
                "rama adoptiva",
                "★",
                "→",
                "ex-pareja de madre adoptiva",
                false));
    }

    @Test
    public void testFormatInSpanish_Parent_8_InLaw_Sep() {
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
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER))
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
                null,
                "★",
                "→",
                "ex-pareja de heptabuela",
                false));
    }

    @Test
    public void testFormatInSpanish_Parent_11_InLaw_Adopt_Sep() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(11)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER))
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
                "rama adoptiva",
                "★",
                "→",
                "ex-pareja de decabuela  (ex-pareja de ancestro directo de 11 generaciones)",
                false));
    }

    @Test
    public void testFormatInSpanish_Child_1_InLaw() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.CHILD)
                .generation(1)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.DESCENDANT, TreeSideType.MOTHER))
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
                null,
                "★",
                "↘",
                "yerno",
                false));
    }

    @Test
    public void testFormatInSpanish_Child_1_InLaw_Adopt() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.CHILD)
                .generation(1)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.DESCENDANT, TreeSideType.MOTHER))
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
                "rama adoptiva",
                "★",
                "↘",
                "pareja de hija adoptiva",
                false));
    }

    @Test
    public void testFormatInSpanish_Child_1_InLaw_Adopt_Sep() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.CHILD)
                .generation(1)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.DESCENDANT, TreeSideType.MOTHER))
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
                "rama adoptiva",
                "★",
                "↘",
                "ex-pareja de hija adoptiva",
                false));
    }

    @Test
    public void testFormatInSpanish_Child_2_InLaw_Sep() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.CHILD)
                .generation(2)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.DESCENDANT, TreeSideType.MOTHER))
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
                null,
                "★",
                "↘",
                "ex-proyerno",
                false));
    }

    @Test
    public void testFormatInSpanish_Child_8_InLaw_Adopt_Sep() {
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
                .treeSides(Set.of(TreeSideType.DESCENDANT, TreeSideType.MOTHER))
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
                "rama adoptiva",
                "★",
                "↘",
                "ex-pareja de heptanieta",
                false));
    }

    @Test
    public void testFormatInSpanish_Child_11_InLaw_Adopt_Sep() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.CHILD)
                .generation(11)
                .grade(0)
                .isInLaw(true)
                .isHalf(false)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(true)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.DESCENDANT, TreeSideType.MOTHER))
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
                "rama adoptiva",
                "★",
                "↘",
                "ex-pareja de decanieta  (ex-pareja de descendiente directo de 11 generaciones)",
                false));
    }

    @Test
    public void testFormatInSpanish_Sibling_InLaw_Adopt() {
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
                .isHalf(false)
                .adoptionType(AdoptionType.ADOPTIVE)
                .spouseSex(SexType.F)
                .isSeparated(false)
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
                "rama adoptiva",
                "★",
                "↔",
                "cuñado",
                false));
    }

    @Test
    public void testFormatInSpanish_Sibling_InLaw_Half_Adopt_Sep() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-hermana",
                false));
    }

    @Test
    public void testFormatInSpanish_Cousin_1() {
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
                .grade(1)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
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
                null,
                null,
                "★",
                "↔",
                "primo",
                false));
    }

    @Test
    public void testFormatInSpanish_Cousin_4_InLaw_Half_Adopt_Sep() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-prima 4ta",
                false));
    }

    @Test
    public void testFormatInSpanish_Pibling_1_1() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-tía",
                false));
    }

    @Test
    public void testFormatInSpanish_Nibling_1_1() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-sobrina",
                false));
    }

    @Test
    public void testFormatInSpanish_Pibling_1_4() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-tía 4ta  (ex-pareja de medio-prima 3ra de padre/madre)",
                false));
    }

    @Test
    public void testFormatInSpanish_Nibling_1_4() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-sobrina 4ta  (ex-pareja de hija de medio-primo/a 3ro/a)",
                false));
    }

    @Test
    public void testFormatInSpanish_Pibling_8_1() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-tía-heptabuela  (ex-pareja de medio-hermana de heptabuelo/a)",
                false));
    }

    @Test
    public void testFormatInSpanish_Nibling_8_1() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-sobrina-heptanieta  (ex-pareja de heptanieta de medio-hermano/a)",
                false));
    }

    @Test
    public void testFormatInSpanish_Pibling_8_4() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-tía-heptabuela 4ta  (ex-pareja de medio-prima 3ra de heptabuelo/a)",
                false));
    }

    @Test
    public void testFormatInSpanish_Nibling_8_4() {
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
                "rama adoptiva",
                "★",
                "↔",
                "ex-pareja de medio-sobrina-heptanieta 4ta  (ex-pareja de heptanieta de medio-primo/a 3ro/a)",
                false));
    }

    // ── sex symbol ───────────────────────────────────────────────────────────

    @Test
    public void testFormatInSpanish_Self_Female() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.F)
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
                .spouseSex(SexType.M)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(null)
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♀",
                "✝",
                "~1823",
                "Argentina",
                null,
                null,
                "★",
                " ",
                "persona principal",
                false));
    }

    // ── alive / death marker ──────────────────────────────────────────────────

    @Test
    public void testFormatInSpanish_Self_Alive() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(true)
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
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(null)
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♂",
                " ",
                "~1823",
                "Argentina",
                null,
                null,
                "★",
                " ",
                "persona principal",
                false));
    }

    // ── birth year formatting ─────────────────────────────────────────────────

    @Test
    public void testFormatInSpanish_Self_ExactYear() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(false)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.SELF)
                .generation(0)
                .grade(-1)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
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
                "1823",
                "Argentina",
                null,
                null,
                "★",
                " ",
                "persona principal",
                false));
    }

    @Test
    public void testFormatInSpanish_Self_NoYear() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(null)
                .personYearOfBirthIsAbout(false)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.SELF)
                .generation(0)
                .grade(-1)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
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
                null,
                "Argentina",
                null,
                null,
                "★",
                " ",
                "persona principal",
                false));
    }

    // ── distinguished / obfuscated ────────────────────────────────────────────

    @Test
    public void testFormatInSpanish_Self_NotDistinguished() {
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
                .isSeparated(false)
                .isDistinguishedPerson(false)
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
                null,
                "",
                " ",
                "persona principal",
                false));
    }

    @Test
    public void testFormatInSpanish_Self_Obfuscated() {
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
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(null)
                .isObfuscated(true)
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
                null,
                "★",
                " ",
                "persona principal",
                true));
    }

    // ── PARENT direct (no in-law) ─────────────────────────────────────────────

    @Test
    public void testFormatInSpanish_Parent_1_Female() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.F)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(1)
                .grade(0)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.M)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♀",
                "✝",
                "~1823",
                "Argentina",
                null,
                null,
                "★",
                "→",
                "madre",
                false));
    }

    @Test
    public void testFormatInSpanish_Parent_2() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(2)
                .grade(0)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.FATHER))
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
                null,
                "★",
                "←",
                "abuelo",
                false));
    }

    @Test
    public void testFormatInSpanish_Parent_2_Female() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.F)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(2)
                .grade(0)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.M)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♀",
                "✝",
                "~1823",
                "Argentina",
                null,
                null,
                "★",
                "→",
                "abuela",
                false));
    }

    // ── CHILD direct (no in-law) ──────────────────────────────────────────────

    @Test
    public void testFormatInSpanish_Child_1() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.CHILD)
                .generation(1)
                .grade(0)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.DESCENDANT, TreeSideType.FATHER))
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
                null,
                "★",
                "↙",
                "hijo",
                false));
    }

    @Test
    public void testFormatInSpanish_Child_1_Female() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.F)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.CHILD)
                .generation(1)
                .grade(0)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.M)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.DESCENDANT, TreeSideType.MOTHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♀",
                "✝",
                "~1823",
                "Argentina",
                null,
                null,
                "★",
                "↘",
                "hija",
                false));
    }

    // ── SIBLING direct (no in-law) ────────────────────────────────────────────

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
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
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
                null,
                null,
                "★",
                "↔",
                "hermano",
                false));
    }

    @Test
    public void testFormatInSpanish_Sibling_Female() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.F)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.SIBLING)
                .generation(0)
                .grade(0)
                .isInLaw(false)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.M)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER, TreeSideType.FATHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♀",
                "✝",
                "~1823",
                "Argentina",
                null,
                null,
                "★",
                "↔",
                "hermana",
                false));
    }

    @Test
    public void testFormatInSpanish_Sibling_Half_Female() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.F)
                .personIsAlive(false)
                .personName("Juan \"Loco\" Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(true)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.SIBLING)
                .generation(0)
                .grade(0)
                .isInLaw(false)
                .isHalf(true)
                .adoptionType(null)
                .spouseSex(SexType.M)
                .isSeparated(false)
                .isDistinguishedPerson(true)
                .treeSides(Set.of(TreeSideType.MOTHER))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1",
                "Juan \"Loco\" Pérez",
                "♀",
                "✝",
                "~1823",
                "Argentina",
                null,
                null,
                "★",
                "→",
                "medio-hermana",
                false));
    }

    // ── PARENT spouse-family (suegro/suegra, abuelo político, etc.) ──────────

    @Test
    public void testFormatInSpanish_Parent_1_SpouseFamily_Male() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan Pérez")
                .personYearOfBirth(1823)
                .personYearOfBirthIsAbout(false)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(1)
                .grade(0)
                .isInLaw(true)
                .isSpouseFamily(true)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
                .isDistinguishedPerson(false)
                .treeSides(Set.of(TreeSideType.SPOUSE))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1", "Juan Pérez", "♂", "✝", "1823", "Argentina",
                null, null, "", "◇", "suegro", false));
    }

    @Test
    public void testFormatInSpanish_Parent_1_SpouseFamily_Female() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(1)
                .personSex(SexType.F)
                .personIsAlive(false)
                .personName("María Pérez")
                .personYearOfBirth(1830)
                .personYearOfBirthIsAbout(false)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(1)
                .grade(0)
                .isInLaw(true)
                .isSpouseFamily(true)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.M)
                .isSeparated(false)
                .isDistinguishedPerson(false)
                .treeSides(Set.of(TreeSideType.SPOUSE))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "1", "María Pérez", "♀", "✝", "1830", "Argentina",
                null, null, "", "◇", "suegra", false));
    }

    @Test
    public void testFormatInSpanish_Parent_2_SpouseFamily_Male() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(2)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan Pérez")
                .personYearOfBirth(1800)
                .personYearOfBirthIsAbout(false)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(2)
                .grade(0)
                .isInLaw(true)
                .isSpouseFamily(true)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
                .isDistinguishedPerson(false)
                .treeSides(Set.of(TreeSideType.SPOUSE))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "2", "Juan Pérez", "♂", "✝", "1800", "Argentina",
                null, null, "", "◇", "abuelo político", false));
    }

    @Test
    public void testFormatInSpanish_Parent_2_SpouseFamily_Female() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(2)
                .personSex(SexType.F)
                .personIsAlive(false)
                .personName("María Pérez")
                .personYearOfBirth(1800)
                .personYearOfBirthIsAbout(false)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(2)
                .grade(0)
                .isInLaw(true)
                .isSpouseFamily(true)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.M)
                .isSeparated(false)
                .isDistinguishedPerson(false)
                .treeSides(Set.of(TreeSideType.SPOUSE))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "2", "María Pérez", "♀", "✝", "1800", "Argentina",
                null, null, "", "◇", "abuela política", false));
    }

    @Test
    public void testFormatInSpanish_Parent_11_SpouseFamily_Male() {
        RelationshipDto relationshipDto = RelationshipDto.builder()
                .personIndex(3)
                .personSex(SexType.M)
                .personIsAlive(false)
                .personName("Juan Pérez")
                .personYearOfBirth(1600)
                .personYearOfBirthIsAbout(false)
                .personCountryOfBirth("Argentina")
                .referenceType(ReferenceType.PARENT)
                .generation(11)
                .grade(0)
                .isInLaw(true)
                .isSpouseFamily(true)
                .isHalf(false)
                .adoptionType(null)
                .spouseSex(SexType.F)
                .isSeparated(false)
                .isDistinguishedPerson(false)
                .treeSides(Set.of(TreeSideType.SPOUSE))
                .isObfuscated(false)
                .build();

        FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);

        assertThat(formattedRelationship).isEqualTo(new FormattedRelationship(
                "3", "Juan Pérez", "♂", "✝", "1600", "Argentina",
                null, null, "", "◇",
                "decabuelo político  (ancestro político de 11 generaciones)",
                false));
    }

    // ── RELATIVE ──────────────────────────────────────────────────────────────

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
                "rama adoptiva",
                "★",
                "↔",
                "familiar",
                false));
    }

}
