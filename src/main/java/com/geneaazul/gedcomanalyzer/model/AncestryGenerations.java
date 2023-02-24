package com.geneaazul.gedcomanalyzer.model;

public record AncestryGenerations(int ascending, int descending) {

    public static AncestryGenerations of(int ascending, int descending) {
        return new AncestryGenerations(ascending, descending);
    }

    public static AncestryGenerations empty() {
        return new AncestryGenerations(0, 0);
    }

}
