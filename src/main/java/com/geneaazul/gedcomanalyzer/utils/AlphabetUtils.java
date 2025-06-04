package com.geneaazul.gedcomanalyzer.utils;

import com.ibm.icu.text.Transliterator;
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Nullable;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AlphabetUtils {

    private static final String ARABIC_TO_LATIN_ID = "Arabic-Latin/BGN";
    private static final String GREEK_TO_LATIN_ID = "Greek-Latin/BGN";
    private static final String HEBREW_TO_LATIN_ID = "Hebrew-Latin/BGN";
    private static final String RUSSIAN_TO_LATIN_ID = "Russian-Latin/BGN";

    private static final Transliterator ARABIC_TO_LATIN_TRANS = Transliterator.getInstance(ARABIC_TO_LATIN_ID);
    private static final Transliterator GREEK_TO_LATIN_TRANS = Transliterator.getInstance(GREEK_TO_LATIN_ID);
    private static final Transliterator HEBREW_TO_LATIN_TRANS = Transliterator.getInstance(HEBREW_TO_LATIN_ID);
    private static final Transliterator RUSSIAN_TO_LATIN_TRANS = Transliterator.getInstance(RUSSIAN_TO_LATIN_ID);

    /*
     * і (U+0456) -> i
     */
    private static final String[] SPECIAL_CHARS_SEARCH = { "і" };
    private static final String[] SPECIAL_CHARS_REPLACEMENT = { "i" };

    public static String convertAnyToLatin(@Nullable String str) {
        if (StringUtils.isBlank(str)) {
            return str;
        }

        str = ARABIC_TO_LATIN_TRANS.transform(str);
        str = GREEK_TO_LATIN_TRANS.transform(str);
        str = HEBREW_TO_LATIN_TRANS.transform(str);
        str = RUSSIAN_TO_LATIN_TRANS.transform(str);
        return StringUtils.replaceEach(str, SPECIAL_CHARS_SEARCH, SPECIAL_CHARS_REPLACEMENT);
    }

}
