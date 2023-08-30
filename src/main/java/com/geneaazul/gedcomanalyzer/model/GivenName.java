package com.geneaazul.gedcomanalyzer.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

public record GivenName(
        String value,
        String normalized,
        int wordsCount,
        Pattern searchPattern) {

    public static GivenName of(String value, String normalized) {

        // Use normalized value to generate the matching regex
        String[] words = StringUtils.splitByWholeSeparator(normalized, " ");
        String regex;

        if (words.length == 1) {
            regex = "\\b" + words[0] + "\\b";
        } else {
            regex = Arrays.stream(words)
                    .map(word -> "(?=.*\\b" + word + "\\b)")
                    .collect(Collectors.joining("", "^", ".*$"));
        }

        return new GivenName(value, normalized, words.length, Pattern.compile(regex));
    }

    public boolean matches(@Nullable GivenName other) {
        if (other == null) {
            return false;
        }

        if (this.wordsCount == 1 && other.wordsCount == 1) {
            return this.normalized.equals(other.normalized);
        }

        if (this.wordsCount <= other.wordsCount) {
            return this.searchPattern.matcher(other.normalized).find();
        } else {
            return other.searchPattern.matcher(this.normalized).find();
        }
    }

}
