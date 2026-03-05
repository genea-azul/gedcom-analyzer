package com.geneaazul.gedcomanalyzer.utils;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SetUtilsTests {

    @Test
    void merge_returnsFirstWhenSecondIsNull() {
        Set<String> s1 = Set.of("a", "b");
        assertThat(SetUtils.merge(s1, null)).isSameAs(s1);
    }

    @Test
    void merge_returnsSecondWhenFirstIsNull() {
        Set<String> s2 = Set.of("a", "b");
        assertThat(SetUtils.merge(null, s2)).isSameAs(s2);
    }

    @Test
    void merge_returnsFirstWhenSecondIsEmpty() {
        Set<String> s1 = Set.of("a", "b");
        Set<String> s2 = Set.of();
        assertThat(SetUtils.merge(s1, s2)).isSameAs(s1);
    }

    @Test
    void merge_returnsSecondWhenFirstIsEmpty() {
        Set<String> s1 = Set.of();
        Set<String> s2 = Set.of("a", "b");
        assertThat(SetUtils.merge(s1, s2)).isSameAs(s2);
    }

    @Test
    void merge_returnsSameWhenBothSameReference() {
        Set<String> s = Set.of("a");
        assertThat(SetUtils.merge(s, s)).isSameAs(s);
    }

    @Test
    void merge_returnsFirstWhenFirstContainsAllOfSecond() {
        Set<String> s1 = Set.of("a", "b", "c");
        Set<String> s2 = Set.of("a", "b");
        assertThat(SetUtils.merge(s1, s2)).isSameAs(s1);
    }

    @Test
    void merge_returnsSecondWhenSecondContainsAllOfFirst() {
        Set<String> s1 = Set.of("a", "b");
        Set<String> s2 = Set.of("a", "b", "c");
        assertThat(SetUtils.merge(s1, s2)).isSameAs(s2);
    }

    @Test
    void merge_combinesDisjointSets() {
        Set<String> s1 = Set.of("a", "b");
        Set<String> s2 = Set.of("c", "d");
        Set<String> merged = SetUtils.merge(s1, s2);
        assertThat(merged).containsExactlyInAnyOrder("a", "b", "c", "d");
        assertThat(merged).isUnmodifiable();
    }

    @Test
    void merge_combinesOverlappingSets() {
        Set<String> s1 = Set.of("a", "b");
        Set<String> s2 = Set.of("b", "c");
        Set<String> merged = SetUtils.merge(s1, s2);
        assertThat(merged).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void containsAll_returnsTrueWhenSameReference() {
        Set<String> s = Set.of("a");
        assertThat(SetUtils.containsAll(s, s)).isTrue();
    }

    @Test
    void containsAll_returnsFalseWhenFirstIsNull() {
        assertThat(SetUtils.containsAll(null, Set.of("a"))).isFalse();
    }

    @Test
    void containsAll_returnsFalseWhenSecondIsNull() {
        assertThat(SetUtils.containsAll(Set.of("a"), null)).isFalse();
    }

    @Test
    void containsAll_returnsTrueWhenFirstContainsAllElementsOfSecond() {
        assertThat(SetUtils.containsAll(Set.of("a", "b", "c"), Set.of("a", "b"))).isTrue();
    }

    @Test
    void containsAll_returnsFalseWhenFirstDoesNotContainAllOfSecond() {
        assertThat(SetUtils.containsAll(Set.of("a", "b"), Set.of("a", "b", "c"))).isFalse();
    }
}
