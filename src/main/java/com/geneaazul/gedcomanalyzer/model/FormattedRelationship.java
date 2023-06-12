package com.geneaazul.gedcomanalyzer.model;

public record FormattedRelationship(
        String index,
        String personName,
        String personSex,
        String personIsAlive,
        String personYearOfBirth,
        String personCountryOfBirth,
        String treeSide,
        String relationshipDesc) {
}
