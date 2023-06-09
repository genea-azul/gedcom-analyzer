package com.geneaazul.gedcomanalyzer.utils;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SetUtils {

    public static <T> Set<T> merge(Set<T> s1, Set<T> s2) {
        if (s1 == null && s2 == null) {
            return null;
        }
        if (s1 == null) {
            return s2;
        }
        if (s2 == null) {
            return s1;
        }
        return Stream
                .of(s1, s2)
                .flatMap(Set::stream)
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
