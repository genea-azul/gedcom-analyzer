package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MapUtilsTests {

    @Test
    void groupByAndOrderAlphabetically_emptyList_returnsEmptyMap() {
        Map<String, List<String>> result = MapUtils.groupByAndOrderAlphabetically(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void groupByAndOrderAlphabetically_groupsAndOrdersValues() {
        List<Pair<String, Optional<String>>> opts = List.of(
                Pair.of("k1", Optional.of("zebra")),
                Pair.of("k1", Optional.of("alpha")),
                Pair.of("k2", Optional.of("beta")),
                Pair.of("k1", Optional.of("alpha"))
        );
        Map<String, List<String>> result = MapUtils.groupByAndOrderAlphabetically(opts);
        assertThat(result).containsOnlyKeys("k1", "k2");
        assertThat(result.get("k1")).containsExactly("alpha", "zebra");
        assertThat(result.get("k2")).containsExactly("beta");
    }

    @Test
    void groupByAndOrderAlphabetically_skipsEmptyOptionals() {
        List<Pair<String, Optional<String>>> opts = List.of(
                Pair.of("k1", Optional.<String>empty()),
                Pair.of("k1", Optional.of("a"))
        );
        Map<String, List<String>> result = MapUtils.groupByAndOrderAlphabetically(opts);
        assertThat(result.get("k1")).containsExactly("a");
    }

    @Test
    void merge_returnsFirstWhenSecondIsNull() {
        Map<String, Set<Integer>> m1 = Map.of("a", Set.of(1));
        assertThat(MapUtils.merge(m1, null)).isSameAs(m1);
    }

    @Test
    void merge_returnsSecondWhenFirstIsNull() {
        Map<String, Set<Integer>> m2 = Map.of("a", Set.of(1));
        assertThat(MapUtils.merge(null, m2)).isSameAs(m2);
    }

    @Test
    void merge_returnsFirstWhenSecondIsEmpty() {
        Map<String, Set<Integer>> m1 = Map.of("a", Set.of(1));
        Map<String, Set<Integer>> m2 = Map.of();
        assertThat(MapUtils.merge(m1, m2)).isSameAs(m1);
    }

    @Test
    void merge_returnsSameWhenBothSameReference() {
        Map<String, Set<Integer>> m = new HashMap<>(Map.of("a", Set.of(1)));
        assertThat(MapUtils.merge(m, m)).isSameAs(m);
    }

    @Test
    void merge_combinesMapsAndMergesOverlappingKeys() {
        Map<String, Set<String>> m1 = Map.of("x", Set.of("a", "b"));
        Map<String, Set<String>> m2 = Map.of("x", Set.of("b", "c"), "y", Set.of("d"));
        Map<String, Set<String>> result = MapUtils.merge(m1, m2);
        assertThat(result).containsOnlyKeys("x", "y");
        assertThat(result.get("x")).containsExactlyInAnyOrder("a", "b", "c");
        assertThat(result.get("y")).containsExactly("d");
    }

    @Test
    void orderHierarchically_ordersByCardinalityDescThenByKey() {
        List<String> values = List.of("a", "b", "a", "c", "a");
        List<Triple<String, Integer, List<String>>> result = MapUtils.orderHierarchically(
                values,
                s -> s,
                s -> s
        );
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getLeft()).isEqualTo("a");
        assertThat(result.get(0).getMiddle()).isEqualTo(3);
        assertThat(result.get(1).getLeft()).isEqualTo("b");
        assertThat(result.get(1).getMiddle()).isEqualTo(1);
        assertThat(result.get(2).getLeft()).isEqualTo("c");
        assertThat(result.get(2).getMiddle()).isEqualTo(1);
    }

    @Test
    void orderHierarchically_withNullMapper_usesEmptyGroupedElements() {
        List<String> values = List.of("a", "a");
        List<Triple<String, Integer, List<Object>>> result = MapUtils.orderHierarchically(
                values,
                s -> s,
                null
        );
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLeft()).isEqualTo("a");
        assertThat(result.get(0).getMiddle()).isEqualTo(2);
        assertThat(result.get(0).getRight()).isNull();
    }
}
