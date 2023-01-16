package com.geneaazul.gedcomanalyzer.model;

public record Surname(
        String value,
        String simplifiedMainWord,
        String normalizedMainWord) {

    public static Surname of(String value, String simplifiedMainWord, String normalizedMainWord) {
        return new Surname(value, simplifiedMainWord, normalizedMainWord);
    }

    public boolean matches(Surname other) {
        if (other == null) {
            return false;
        }

        if (!this.normalizedMainWord.equals(other.normalizedMainWord)) {
            return false;
        }

        if (isAmbiguousNormalized(this.normalizedMainWord)) {
            return this.simplifiedMainWord.equals(other.simplifiedMainWord);
        }

        return true;
    }

    private boolean isAmbiguousNormalized(String normalized) {
        return normalized.length() <= 3 && normalized.endsWith("_");
    }

}
