package com.geneaazul.gedcomanalyzer.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.Getter;

@Getter
public class GivenName {

    private final String name;

    private final int wordsCount;

    private final Pattern searchPattern;

    public GivenName(String name) {
        this.name = name;

        String[] words = StringUtils.splitByWholeSeparator(name, " ");
        String regex;

        if (words.length == 1) {
            regex = "\\b" + words[0] + "\\b";
        } else {
            regex = Arrays.stream(words)
                    .map(word -> "(?=.*\\b" + word + "\\b)")
                    .collect(Collectors.joining("", "^", ".*$"));
        }

        this.wordsCount = words.length;
        this.searchPattern = Pattern.compile(regex);
    }

    public static GivenName of(String name) {
        return new GivenName(name);
    }

}
