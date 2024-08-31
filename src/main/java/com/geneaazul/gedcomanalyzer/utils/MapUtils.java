package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MapUtils {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static <K, T extends Comparable<T>> Map<K, List<T>> groupByAndOrderAlphabetically(List<Pair<K, Optional<T>>> opts) {
        return opts
                .stream()
                .filter(pair -> pair.getRight().isPresent())
                .collect(Collectors.groupingBy(
                        Pair::getLeft,
                        Collectors.mapping(
                                pair -> pair.getRight().get(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        values -> values
                                                .stream()
                                                .distinct()
                                                .sorted()
                                                .toList()))));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static <K, T, C extends Comparable<C>, D> Map<K, List<Triple<C, Integer, List<D>>>> groupByAndOrderHierarchically(
            List<Pair<K, Optional<T>>> opts,
            Function<T, C> subElementsKeyExtractor,
            @Nullable Function<T, D> groupedSubElementsMapper) {
        return opts
                .stream()
                .filter(pair -> pair.getRight().isPresent())
                .collect(Collectors.groupingBy(
                        Pair::getLeft,
                        Collectors.mapping(
                                pair -> pair.getRight().get(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        values -> {
                                            List<C> subElementKeys = values
                                                    .stream()
                                                    .map(subElementsKeyExtractor)
                                                    .toList();
                                            Map<C, Integer> cardinality = CollectionUtils.getCardinalityMap(subElementKeys);
                                            Map<C, List<D>> groupedSubElements = (groupedSubElementsMapper == null)
                                                    ? Map.of()
                                                    : values
                                                            .stream()
                                                            .collect(Collectors.groupingBy(subElementsKeyExtractor, Collectors.mapping(groupedSubElementsMapper, Collectors.toList())));

                                            return cardinality
                                                    .entrySet()
                                                    .stream()
                                                    .sorted(Comparator
                                                            .<Map.Entry<C, Integer>>comparingInt(Map.Entry::getValue)
                                                            .reversed()
                                                            .thenComparing(Map.Entry::getKey))
                                                    .map(entry -> Triple.of(
                                                            entry.getKey(),
                                                            entry.getValue(),
                                                            groupedSubElements.get(entry.getKey())))
                                                    .toList();
                                        }))));
    }

    public static <T, M extends Map<String, Set<T>>> M merge(M m1, M m2) {
        if (m1 == m2) {
            return m1;
        }
        if (m1 == null || m1.isEmpty()) {
            return m2;
        }
        if (m2 == null || m2.isEmpty()) {
            return m1;
        }
        Map<String, Set<T>> result = new HashMap<>(m1);
        m2
                .forEach((key, value) -> result.merge(key, value, SetUtils::merge));
        //noinspection unchecked
        return (M) result;
    }

}
