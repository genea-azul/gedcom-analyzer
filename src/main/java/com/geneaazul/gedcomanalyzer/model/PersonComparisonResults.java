package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.service.SearchService;

import java.util.List;

public record PersonComparisonResults(
        EnrichedPerson person,
        List<PersonComparisonResult> results,
        int minScore) {

    public static PersonComparisonResults of(EnrichedPerson person, List<PersonComparisonResult> comparisonResults) {
        int minScore = comparisonResults
                .stream()
                .mapToInt(PersonComparisonResult::getScore)
                .min()
                .orElse(SearchService.NOT_A_DUPLICATE_SCORE);
        return new PersonComparisonResults(person, comparisonResults, minScore);
    }

}
