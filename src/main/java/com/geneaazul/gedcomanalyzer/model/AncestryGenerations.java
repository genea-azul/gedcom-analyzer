package com.geneaazul.gedcomanalyzer.model;

public record AncestryGenerations(Integer ascending, Integer descending) {

    public static AncestryGenerations of(Integer ascending, Integer descending) {
        return new AncestryGenerations(ascending, descending);
    }

    public static AncestryGenerations empty() {
        return new AncestryGenerations(0, 0);
    }

}
