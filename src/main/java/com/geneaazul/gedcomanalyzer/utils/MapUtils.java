package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MapUtils {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static <T> Map<String, Set<T>> reduceOptsToSet(List<Pair<String, Optional<T>>> opts) {
        return opts
                .stream()
                .filter(pair -> pair.getRight().isPresent())
                .collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(pair -> pair.getRight().get(), Collectors.toSet())));
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
