package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.service.SearchService;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PersonComparisonResults {

    private final EnrichedPerson person;
    private final List<PersonComparisonResult> results;
    private final int minScore;

    public static PersonComparisonResults of(EnrichedPerson person, List<PersonComparisonResult> comparisonResults) {
        int minScore = comparisonResults
                .stream()
                .mapToInt(PersonComparisonResult::getScore)
                .min()
                .orElse(SearchService.NOT_A_DUPLICATE_SCORE);
        return new PersonComparisonResults(person, comparisonResults, minScore);
    }

}
