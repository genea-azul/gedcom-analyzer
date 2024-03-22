package com.geneaazul.gedcomanalyzer.utils;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SetUtils {

    public static <T, S extends Set<T>> S merge(S s1, S s2) {
        if (s1 == s2) {
            return s1;
        }
        if (s1 == null || s1.isEmpty()) {
            return s2;
        }
        if (s2 == null || s2.isEmpty()) {
            return s1;
        }
        if (s1.size() >= s2.size() && s1.containsAll(s2)) {
            return s1;
        }
        if (s2.size() > s1.size() && s2.containsAll(s1)) {
            return s2;
        }
        //noinspection unchecked
        return (S) Stream
                .concat(s1.stream(), s2.stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static <T> boolean containsAll(Set<T> s1, Set<T> s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.containsAll(s2);
    }

}
