package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;

import jakarta.annotation.Nullable;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NameUtils {

    private static final String[] NAME_SEARCH_SPECIAL_CHARS      = { "?", "(", ")", "'", ".", "-" };
    private static final String[] NAME_REPLACEMENT_SPECIAL_CHARS = {  "",  "",  "",  "",  "", " " };
    private static final Pattern NAME_MULTIPLE_SPACES_PATTERN = Pattern.compile("  +");

    private static final Pattern NORMALIZED_NAME_SEPARATOR_PATTERN = Pattern.compile("-");

    private static final String[] GIVEN_NAME_COMMON_EXTRAS             = { " cadette", " cadete", " cadet", " detto ", " dita ", " dite ", " dit " };
    private static final String[] GIVEN_NAME_COMMON_EXTRAS_REPLACEMENT = {         "",        "",       "",       " ",      " ",      " ",     " " };

    private static final String[] SURNAME_SPECIAL_CASES             = { "fernandes vilegas", "lopes osornio" };
    private static final String[] SURNAME_SPECIAL_CASES_REPLACEMENT = { "fernandesvilegas", "lopesosornio" };

    private static final Pattern SURNAME_COMMON_CONNECTOR_PATTERN =
             Pattern.compile("^([^ ]+)(?: (da|de|di|do|du|del|der|della|delle|dall|das|dos|la|le|lo|las|los|san))+ (.+)$");
    private static final Pattern SURNAME_COMMON_PREFIX_PATTERN =
            Pattern.compile("^([a-z]|ahets|da|de|di|do|du|del|der|della|delle|dall|das|dos|la|le|lo|las|los|mc|mac|oro|san|sant|santa|santo|saint|sainte|st|van|vander|von) +((?!y +).*)$");
    private static final Pattern SURNAME_DOUBLE_LETTERS_PATTERN = Pattern.compile("([a-z])\\1+");
    private static final Pattern SURNAME_VOWELS_ENDING_PATTERN = Pattern.compile("[aeiou]+$");
    private static final String SURNAME_VOWELS_ENDING_REPLACEMENT = "_";
    private static final String[] SURNAME_SEARCH_CHARS      = { "b", "ç", "ce", "ci", "je", "ji", "k", "q", "y", "z" };
    private static final String[] SURNAME_REPLACEMENT_CHARS = { "v", "c", "se", "si", "ge", "gi", "c", "c", "i", "s" }; // replacement of k and q must be done after replacement of ce and ci

    /**
     * Usually used for givenName and surname simplification.
     */
    @CheckForNull
    public static String simplifyName(@Nullable String name) {
        name = StringUtils.stripAccents(name);
        name = StringUtils.lowerCase(name);
        name = StringUtils.replaceEach(name, NAME_SEARCH_SPECIAL_CHARS, NAME_REPLACEMENT_SPECIAL_CHARS);
        name = RegExUtils.replaceAll(name, NAME_MULTIPLE_SPACES_PATTERN, " ");
        name = StringUtils.trimToNull(name);
        return name;
    }

    /**
     * Usually used for a.k.a. simplification.
     */
    @CheckForNull
    public static String simplifyNameWithAlphabetConversion(@Nullable String name) {
        name = AlphabetUtils.convertAnyToLatin(name);
        name = simplifyName(name);
        return name;
    }

    @CheckForNull
    public static String normalizeGivenName(
            @Nullable String givenName,
            @Nullable SexType sex,
            Map<NameAndSex, String> normalizedGivenNamesMap) {
        if (givenName == null) {
            return null;
        }
        if (StringUtils.containsAny(givenName, GIVEN_NAME_COMMON_EXTRAS)) {
            givenName = StringUtils.replaceEach(givenName, GIVEN_NAME_COMMON_EXTRAS, GIVEN_NAME_COMMON_EXTRAS_REPLACEMENT);
            givenName = RegExUtils.replaceAll(givenName, NAME_MULTIPLE_SPACES_PATTERN, " ");
            givenName = StringUtils.trimToNull(givenName);
        }
        if (sex == null) {
            return givenName;
        }
        String[] words = StringUtils.splitByWholeSeparator(givenName, " ");
        return Arrays.stream(words)
                .map(word -> normalizedGivenNamesMap.getOrDefault(new NameAndSex(word, sex), word))
                .collect(Collectors.joining(" "));
    }

    /**
     * di yannibelli rago  ->  di diyannibelli rago  (concat common connector)
     * di yannibelli rago  ->  diyannibelli rago     (concat common prefix)
     *  diyannibelli rago  ->  diyannibelli          (consider only first word)
     *       diyannivelli  ->  diiannivelli          (replace b with v, replace y with i)
     *       diiannivelli  ->  dianiveli             (remove repeated letters)
     *          dianiveli  ->  ciamiveli             (get optional replacement from NORMALIZED_SURNAMES_MAP)
     */
    @CheckForNull
    public static String normalizeSurnameToMainWord(
            @Nullable String surname,
            Map<String, String> normalizedSurnamesMap) {
        if (surname == null) {
            return null;
        }
        surname = RegExUtils.replaceAll(surname, SURNAME_COMMON_CONNECTOR_PATTERN, "$1$2$3");
        surname = RegExUtils.replaceAll(surname, SURNAME_COMMON_PREFIX_PATTERN, "$1$2");
        surname = StringUtils.replaceEach(surname, SURNAME_SPECIAL_CASES, SURNAME_SPECIAL_CASES_REPLACEMENT);
        surname = StringUtils.substringBefore(surname, " ");
        surname = RegExUtils.replaceAll(surname, SURNAME_DOUBLE_LETTERS_PATTERN, "$1");
        surname = StringUtils.replaceEach(surname, SURNAME_SEARCH_CHARS, SURNAME_REPLACEMENT_CHARS);
        surname = RegExUtils.replaceAll(surname, SURNAME_DOUBLE_LETTERS_PATTERN, "$1");
        return Optional.ofNullable(surname)
                .map(normalizedSurnamesMap::get)
                .orElse(surname);
    }

    /**
     *          ciamiveli  ->  ciamivel_           (replace last vowels with a _)
     */
    @CheckForNull
    public static String shortenSurnameMainWord(@Nullable String surnameMainWord) {
        return RegExUtils.replaceAll(surnameMainWord, SURNAME_VOWELS_ENDING_PATTERN, SURNAME_VOWELS_ENDING_REPLACEMENT);
    }

    public static Map<NameAndSex, String> invertGivenNamesMap(
            Map<String, List<String>> normalizedMascGivenNames,
            Map<String, List<String>> normalizedFemGivenNames) {

        Map<NameAndSex, String> m = invertNamesMap(normalizedMascGivenNames, givenName -> new NameAndSex(givenName, SexType.M));
        Map<NameAndSex, String> f = invertNamesMap(normalizedFemGivenNames, givenName -> new NameAndSex(givenName, SexType.F));
        m.putAll(f);

        return Map.copyOf(m);
    }

    public static Map<String, String> invertSurnamesMap(
            Map<String, List<String>> normalizedSurnames) {

        Map<String, String> inverted = invertNamesMap(normalizedSurnames, Function.identity());

        return Map.copyOf(inverted);
    }

    public static Map<String, String> buildNamePrefixesMap(
            Map<String, String> namePrefixes) {

        Map<String, String> processedKeys = namePrefixes
                .entrySet()
                .stream()
                .map(EntryStreamUtils.entryKeyMapper(name -> RegExUtils.replaceAll(name, NORMALIZED_NAME_SEPARATOR_PATTERN, " ")))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Map.copyOf(processedKeys);
    }

    private static <T> Map<T, String> invertNamesMap(
            Map<String, List<String>> names,
            Function<String, T> mapper) {

        return names
                .entrySet()
                .stream()
                .map(EntryStreamUtils.entryKeyMapper(name -> RegExUtils.replaceAll(name, NORMALIZED_NAME_SEPARATOR_PATTERN, " ")))
                .flatMap(entry -> entry
                        .getValue()
                        .stream()
                        .map(mapper)
                        .map(value -> Map.entry(value, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
