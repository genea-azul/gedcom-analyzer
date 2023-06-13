package com.geneaazul.gedcomanalyzer.model;

public record AncestryGenerations(int ascending, int descending, int directDescending) {

    public static AncestryGenerations of(int ascending, int descending, int directDescending) {
        return new AncestryGenerations(ascending, descending, directDescending);
    }

    public int getTotalGenerations() {
        return ascending + descending + 1;
    }

    public AncestryGenerations merge(AncestryGenerations other) {
        return merge(other.ascending, other.descending, other.directDescending);
    }

    public AncestryGenerations mergeRelationship(Relationship relationship) {
        int ascending = 0, descending = 0, directDescending = 0;
        int generation = relationship.getGeneration();

        if (generation > 0) {
            // ascending
            ascending = generation;
        } else if (generation < 0) {
            // descending
            descending = -generation;
            directDescending = relationship.isDirect() ? -generation : 0;
        }

        return merge(ascending, descending, directDescending);
    }

    private AncestryGenerations merge(int ascending, int descending, int directDescending) {
        if (this.ascending >= ascending
                && this.descending >= descending
                && this.directDescending >= directDescending) {
            return this;
        }
        return new AncestryGenerations(
                Math.max(this.ascending, ascending),
                Math.max(this.descending, descending),
                Math.max(this.directDescending, directDescending));
    }

    public static AncestryGenerations empty() {
        return new AncestryGenerations(0, 0, 0);
    }

}
