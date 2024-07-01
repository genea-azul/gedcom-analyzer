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

    public static <K, U, V> Function<Map.Entry<U, V>, Map.Entry<K, V>> entryKeyMapper(Function<? super U, ? extends K> mapper) {
        return entry -> Map.entry(mapper.apply(entry.getKey()), entry.getValue());
    }

    public static <K, U, V> Function<Map.Entry<K, U>, Map.Entry<K, V>> entryValueMapper(Function<? super U, ? extends V> mapper) {
        return entry -> Map.entry(entry.getKey(), mapper.apply(entry.getValue()));
    }

    public static <K, U, V> Function<Map.Entry<K, U>, Map.Entry<K, V>> entryValueMapper(BiFunction<K, ? super U, ? extends V> mapper) {
        return entry -> Map.entry(entry.getKey(), mapper.apply(entry.getKey(), entry.getValue()));
    }

}
