package com.geneaazul.gedcomanalyzer.model;

public record Surname(
        String value,
        String normalizedMainWord,
        String shortenedMainWord) {

    public static Surname of(String value, String normalizedMainWord, String shortenedMainWord) {
        return new Surname(value, normalizedMainWord, shortenedMainWord);
    }

    public boolean matches(Surname other) {
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
        return shortened.length() <= 3 && shortened.endsWith("_");
    }

}
