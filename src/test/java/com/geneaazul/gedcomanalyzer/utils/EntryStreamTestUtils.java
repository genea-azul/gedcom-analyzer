package com.geneaazul.gedcomanalyzer.utils;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class EntryStreamTestUtils {

    public static <T> void assertEqualKeyValue(Map.Entry<? extends T, ? extends T> entry) {
        assertThat(entry.getKey()).isEqualTo(entry.getValue());
    }

    public static <T, U> void assertEqualMappingKey(Map.Entry<? extends T, ? extends U> entry, Function<? super T, ? extends U> keyMapper) {
        assertThat(keyMapper.apply(entry.getKey())).isEqualTo(entry.getValue());
    }

}
