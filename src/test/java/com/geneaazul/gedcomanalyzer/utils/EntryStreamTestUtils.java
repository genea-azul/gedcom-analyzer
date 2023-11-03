package com.geneaazul.gedcomanalyzer.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EntryStreamTestUtils {

    public static <T> void assertEqualKeyValue(Map.Entry<? extends T, ? extends T> entry) {
        assertThat(entry.getKey()).isEqualTo(entry.getValue());
    }

    public static <T> void assertNotEqualKeyValue(Map.Entry<? extends T, ? extends T> entry) {
        assertThat(entry.getKey()).isNotEqualTo(entry.getValue());
    }

    public static <T, U> void assertEqualMappingKey(Map.Entry<? extends T, ? extends U> entry, Function<? super T, ? extends U> keyMapper) {
        assertThat(keyMapper.apply(entry.getKey())).isEqualTo(entry.getValue());
    }

    public static <T, U> void assertNotEqualMappingKey(Map.Entry<? extends T, ? extends U> entry, Function<? super T, ? extends U> keyMapper) {
        assertThat(keyMapper.apply(entry.getKey())).isNotEqualTo(entry.getValue());
    }

}
