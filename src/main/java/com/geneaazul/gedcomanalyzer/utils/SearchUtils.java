package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SearchUtils {

    private static final String[] NAME_SEARCH_SPECIAL_CHARS = new String[]{ "?", "(", ")", "'", "-" };
    private static final String[] NAME_REPLACEMENT_SPECIAL_CHARS = new String[]{ "", "", "", "", " " };
    private static final Pattern NAME_MULTIPLE_SPACES_PATTERN = Pattern.compile("  +");

    private static final Pattern SURNAME_COMMON_SUFFIX_PATTERN = Pattern.compile("^([a-z]|de|di|la|lo|mc|mac|saint|sainte) +(.*)$");
    private static final Pattern SURNAME_DOUBLE_LETTERS_PATTERN = Pattern.compile("([a-z])\\1+");
    private static final Pattern SURNAME_VOWELS_ENDING_PATTERN = Pattern.compile("[aeiou]+$");
    private static final String SURNAME_VOWELS_ENDING_REPLACEMENT = "_";
    private static final String[] SURNAME_SEARCH_CHARS = new String[]{ "b", "ç", "je", "ji", "y", "z" };
    private static final String[] SURNAME_REPLACEMENT_CHARS = new String[]{ "v", "c", "ge", "gi", "i", "s" };

    public static String simplifyName(@Nullable String name) {
        name = StringUtils.stripAccents(name);
        name = StringUtils.lowerCase(name);
        name = StringUtils.replaceEach(name, NAME_SEARCH_SPECIAL_CHARS, NAME_REPLACEMENT_SPECIAL_CHARS);
        name = RegExUtils.replaceAll(name, NAME_MULTIPLE_SPACES_PATTERN, " ");
        name = StringUtils.trimToNull(name);
        return name;
    }

    public static boolean matchesGivenName(
            @Nullable GivenName name1,
            @Nullable GivenName name2) {

        if (name1 == null || name2 == null) {
            return false;
        }

        if (name1.getWordsCount() == 1 && name2.getWordsCount() == 1) {
            return name1.getName().equals(name2.getName());
        }

        if (name1.getWordsCount() <= name2.getWordsCount()) {
            // In case name1 is whole-word, make a whole-word search in name2
            return name1.getSearchPattern().matcher(name2.getName()).find();
        } else {
            // In case name2 is whole-word, make a whole-word search in name1
            return name2.getSearchPattern().matcher(name1.getName()).find();
        }
    }

    public static String normalizeName(
            @Nullable String name,
            @Nullable SexType sex,
            Map<NameAndSex, String> normalizedNamesMap) {
        if (name == null) {
            return null;
        }
        if (sex == null) {
            return name;
        }
        String[] words = StringUtils.splitByWholeSeparator(name, " ");
        return Arrays.stream(words)
                .map(word -> Optional.ofNullable(normalizedNamesMap.get(new NameAndSex(word, sex)))
                        .orElse(word))
                .collect(Collectors.joining(" "));
    }

    /**
     * di yannibelli rago  ->  diyannibelli rago   (concat common prefix)
     *  diyannibelli rago  ->  diyannibelli        (consider only first word)
     *       diyannivelli  ->  diiannivelli        (replace b with v, replace y with i)
     *       diiannivelli  ->  dianiveli           (remove repeated letters)
     *          dianiveli  ->  dianivel_           (replace last vowels with a _)
     *          dianivel_  ->  ciamivel_           (get optional replacement from NORMALIZED_SURNAMES_MAP)
     */
    public static String normalizeSurname(
            @Nullable String surname,
            Map<String, String> normalizedSurnamesMap) {
        surname = RegExUtils.replaceAll(surname, SURNAME_COMMON_SUFFIX_PATTERN, "$1$2");
        surname = StringUtils.substringBefore(surname, " ");
        surname = StringUtils.replaceEach(surname, SURNAME_SEARCH_CHARS, SURNAME_REPLACEMENT_CHARS);
        surname = RegExUtils.replaceAll(surname, SURNAME_DOUBLE_LETTERS_PATTERN, "$1");
        surname = RegExUtils.replaceAll(surname, SURNAME_VOWELS_ENDING_PATTERN, SURNAME_VOWELS_ENDING_REPLACEMENT);
        surname = Optional.ofNullable(surname)
                .map(normalizedSurnamesMap::get)
                .orElse(surname);
        return surname;
    }

}
