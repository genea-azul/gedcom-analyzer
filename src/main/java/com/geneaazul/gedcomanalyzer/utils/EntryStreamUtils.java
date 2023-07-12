package com.geneaazul.gedcomanalyzer.utils;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EntryStreamUtils {

    public static <T> Function<T, Map.Entry<T, T>> unaryEntry() {
        return value -> Map.entry(value, value);
    }

    public static <T, U> Function<T, Map.Entry<T, U>> mappingEntry(Function<? super T, ? extends U> mapper) {
        return value -> Map.entry(value, mapper.apply(value));
    }

    public static <T, U, V> Function<Map.Entry<T, U>, Map.Entry<T, V>> entryValueMapper(Function<? super U, ? extends V> mapper) {
        return entry -> Map.entry(entry.getKey(), mapper.apply(entry.getValue()));
    }

    public static <T, U, V> Function<Map.Entry<T, U>, Map.Entry<T, V>> entryValueMapper(BiFunction<T, ? super U, ? extends V> mapper) {
        return entry -> Map.entry(entry.getKey(), mapper.apply(entry.getKey(), entry.getValue()));
    }

}
