package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelationshipsTests {

    private EnrichedPerson person1;
    private EnrichedPerson person2;

    @BeforeEach
    void setUp() {
        person1 = mock(EnrichedPerson.class);
        when(person1.getId()).thenReturn(1);
        person2 = mock(EnrichedPerson.class);
        when(person2.getId()).thenReturn(2);
    }

    @Test
    void from_singleRelationship_createsRelationships() {
        Relationship rel = new Relationship(person1, 0, 1, false, false, null, null, null, null);
        Relationships rs = Relationships.from(rel);
        assertThat(rs.getPersonId()).isEqualTo(1);
        assertThat(rs.size()).isEqualTo(1);
        assertThat(rs.isEmpty()).isFalse();
        assertThat(rs.isContainsDirect()).isTrue();
        assertThat(rs.isContainsNotInLaw()).isTrue();
        assertThat(rs.findFirst()).isEqualTo(rel);
        assertThat(rs.findLast()).isEqualTo(rel);
        assertThat(rs.findFirstNotInLaw()).hasValue(rel);
        assertThat(rs.contains(rel)).isTrue();
    }

    @Test
    void isEmpty_whenSingleRelationship_returnsFalse() {
        Relationship rel = Relationship.empty(person1);
        Relationships rs = Relationships.from(rel);
        assertThat(rs.isEmpty()).isFalse();
    }

    @Test
    void merge_samePersonId_mergesRelationships() {
        Relationship rel1 = new Relationship(person1, 0, 1, false, false, null, null, null, null);
        Relationship rel2 = new Relationship(person1, 1, 0, false, false, null, null, null, null);
        Relationships rs1 = Relationships.from(rel1);
        Relationships rs2 = Relationships.from(rel2);
        // Use strategy that does not use "closest distance" so both relationships are kept
        Relationships merged = rs1.merge(rs2, Relationships.VisitedRelationshipTraversalStrategy.SKIP_IN_LAW_WHEN_EXISTS_SAME_DIST_NOT_IN_LAW);
        assertThat(merged.getPersonId()).isEqualTo(1);
        assertThat(merged.size()).isEqualTo(2);
    }

    @Test
    void merge_differentPersonId_throws() {
        Relationship rel1 = new Relationship(person1, 0, 1, false, false, null, null, null, null);
        Relationship rel2 = new Relationship(person2, 1, 0, false, false, null, null, null, null);
        Relationships rs1 = Relationships.from(rel1);
        Relationships rs2 = Relationships.from(rel2);
        assertThatThrownBy(() -> rs1.merge(rs2, Relationships.VisitedRelationshipTraversalStrategy.CLOSEST_SKIPPING_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mergeTreeSides_whenThisContainsAll_returnsThis() {
        Set<TreeSideType> sides = Set.of(TreeSideType.FATHER, TreeSideType.MOTHER);
        Relationship rel = new Relationship(person1, 0, 1, false, false, null, null, sides, null);
        Relationships rs = Relationships.from(rel);
        Relationship rel2 = new Relationship(person1, 1, 0, false, false, null, null, Set.of(TreeSideType.FATHER), null);
        Relationships rs2 = Relationships.from(rel2);
        Relationships merged = rs.mergeTreeSides(rs2);
        assertThat(merged).isSameAs(rs);
    }

    @Test
    void mergeTreeSides_whenOtherHasMore_returnsNewWithMergedSides() {
        Relationship rel = new Relationship(person1, 0, 1, false, false, null, null, Set.of(TreeSideType.FATHER), null);
        Relationship rel2 = new Relationship(person1, 1, 0, false, false, null, null, Set.of(TreeSideType.MOTHER), null);
        Relationships rs = Relationships.from(rel);
        Relationships rs2 = Relationships.from(rel2);
        Relationships merged = rs.mergeTreeSides(rs2);
        assertThat(merged).isNotSameAs(rs);
        assertThat(merged.getTreeSides()).containsExactlyInAnyOrder(TreeSideType.FATHER, TreeSideType.MOTHER);
    }

    @Test
    void containsInLawOf_sameDistanceOneInLaw_returnsTrue() {
        Relationship notInLaw = new Relationship(person1, 1, 1, false, false, null, null, null, null);
        Relationship inLaw = new Relationship(person1, 1, 1, true, false, null, null, null, null);
        Relationships rs = Relationships.from(notInLaw);
        assertThat(rs.containsInLawOf(inLaw)).isTrue();
    }

    @Test
    void containsInLawOf_differentPersonId_throws() {
        Relationship rel = new Relationship(person1, 0, 1, false, false, null, null, null, null);
        Relationship inLawOther = new Relationship(person2, 1, 1, true, false, null, null, null, null);
        Relationships rs = Relationships.from(rel);
        assertThatThrownBy(() -> rs.containsInLawOf(inLawOther))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
