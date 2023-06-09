package com.geneaazul.gedcomanalyzer.model;

import jakarta.annotation.Nullable;

public record Surname(
        String value,
        String normalizedMainWord,
        String shortenedMainWord) {

    private static final int MIN_LENGTH_FOR_NOT_AMBIGUOUS = 4;

    public static Surname of(String value, String normalizedMainWord, String shortenedMainWord) {
        return new Surname(value, normalizedMainWord, shortenedMainWord);
    }

    public boolean matches(@Nullable Surname other) {
        if (other == null) {
            return false;
        }

        if (!this.shortenedMainWord.equals(other.shortenedMainWord)) {
            return false;
        }

        if (isAmbiguousShortened(this.shortenedMainWord)) {
            return this.normalizedMainWord.equals(other.normalizedMainWord);
        }

        return true;
    }

    private boolean isAmbiguousShortened(String shortened) {
        return shortened.length() <= MIN_LENGTH_FOR_NOT_AMBIGUOUS && shortened.endsWith("_");
    }

}
