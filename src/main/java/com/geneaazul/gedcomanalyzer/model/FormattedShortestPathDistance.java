package com.geneaazul.gedcomanalyzer.model;

import jakarta.annotation.Nullable;

public record FormattedShortestPathDistance(
        String givenNameSimplified,
        String surnameSimplified,
        String displayName,
        boolean isRelative,
        @Nullable Integer distance) {

}
