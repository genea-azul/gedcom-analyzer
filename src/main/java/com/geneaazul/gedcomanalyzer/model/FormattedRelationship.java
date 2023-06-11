package com.geneaazul.gedcomanalyzer.model;

public record FormattedRelationship(
        String index,
        String personName,
        String personSex,
        String personIsAlive,
        String personCountry,
        String treeSide,
        String relationshipDesc) {
}
