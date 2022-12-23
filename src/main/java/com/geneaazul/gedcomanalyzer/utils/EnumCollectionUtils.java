package com.geneaazul.gedcomanalyzer.utils;

import java.util.EnumMap;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EnumCollectionUtils {

    public static <K extends Enum<K>> EnumMap<K, Integer> getCardinalityMap(Iterable<? extends K> coll, Class<K> clazz) {
        EnumMap<K, Integer> count = new EnumMap<>(clazz);
        for (K obj : coll) {
            count.merge(obj, 1, Integer::sum);
        }
        return count;
    }

}
