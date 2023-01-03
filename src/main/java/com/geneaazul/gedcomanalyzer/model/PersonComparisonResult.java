package com.geneaazul.gedcomanalyzer.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class PersonComparisonResult {

    private final EnrichedPerson compare;
    private final Integer score;

}
