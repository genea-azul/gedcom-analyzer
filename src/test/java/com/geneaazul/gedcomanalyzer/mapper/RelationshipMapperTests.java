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
