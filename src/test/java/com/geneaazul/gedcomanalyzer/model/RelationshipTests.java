package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelationshipTests {

    private EnrichedPerson person;

    @BeforeEach
    void setUp() {
        person = mock(EnrichedPerson.class);
        when(person.getId()).thenReturn(1);
    }

    @Test
    void empty_createsDirectRelationshipWithZeroDistances() {
        Relationship rel = Relationship.empty(person);
        assertThat(rel.person()).isSameAs(person);
        assertThat(rel.distanceToAncestorRootPerson()).isZero();
        assertThat(rel.distanceToAncestorThisPerson()).isZero();
        assertThat(rel.isInLaw()).isFalse();
        assertThat(rel.isHalf()).isFalse();
        assertThat(rel.isDirect()).isTrue();
        assertThat(rel.getDistance()).isZero();
        assertThat(rel.getGeneration()).isZero();
    }

    @Test
    void getDistance_sumsBothDistances() {
        Relationship rel = new Relationship(person, 2, 3, false, false, null, null, null, null);
        assertThat(rel.getDistance()).isEqualTo(5);
    }

    @Test
    void getGeneration_returnsDifference() {
        Relationship rel = new Relationship(person, 2, 1, false, false, null, null, null, null);
        assertThat(rel.getGeneration()).isEqualTo(1);
    }

    @Test
    void isDirect_trueWhenEitherDistanceIsZero() {
        assertThat(new Relationship(person, 0, 1, false, false, null, null, null, null).isDirect()).isTrue();
        assertThat(new Relationship(person, 1, 0, false, false, null, null, null, null).isDirect()).isTrue();
        assertThat(new Relationship(person, 1, 1, false, false, null, null, null, null).isDirect()).isFalse();
    }

    @Test
    void compareTo_ordersByDistanceThenGeneration() {
        Relationship closer = new Relationship(person, 0, 1, false, false, null, null, null, null);
        Relationship farther = new Relationship(person, 1, 1, false, false, null, null, null, null);
        assertThat(closer.compareTo(farther)).isLessThan(0);
        assertThat(farther.compareTo(closer)).isGreaterThan(0);
    }

    @Test
    void withTreeSides_sameSides_returnsThis() {
        Set<TreeSideType> sides = Set.of(TreeSideType.FATHER);
        Relationship rel = new Relationship(person, 0, 1, false, false, null, null, sides, null);
        assertThat(rel.withTreeSides(sides)).isSameAs(rel);
    }

    @Test
    void withTreeSides_differentSides_returnsNewRelationship() {
        Relationship rel = new Relationship(person, 0, 1, false, false, null, null, Set.of(TreeSideType.FATHER), null);
        Relationship updated = rel.withTreeSides(Set.of(TreeSideType.MOTHER));
        assertThat(updated).isNotSameAs(rel);
        assertThat(updated.treeSides()).containsExactly(TreeSideType.MOTHER);
    }

    @Test
    void isInLawOf_sameDistancesOneInLaw_returnsTrue() {
        Relationship notInLaw = new Relationship(person, 1, 1, false, false, null, null, null, null);
        Relationship inLaw = new Relationship(person, 1, 1, true, false, null, null, null, null);
        assertThat(inLaw.isInLawOf(notInLaw)).isTrue();
        assertThat(notInLaw.isInLawOf(inLaw)).isTrue();
    }

    @Test
    void equals_and_hashCode_usePersonIdAndDistances() {
        EnrichedPerson person2 = mock(EnrichedPerson.class);
        when(person2.getId()).thenReturn(1);
        Relationship r1 = new Relationship(person, 0, 1, false, false, null, null, null, null);
        Relationship r2 = new Relationship(person2, 0, 1, false, false, null, null, null, null);
        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }
}
