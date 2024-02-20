package com.geneaazul.gedcomanalyzer.model;

import jakarta.annotation.Nullable;

public record FormattedDistance(
        String givenNameSimplified,
        String surnameSimplified,
        String displayName,
        @Nullable Integer distance) {

}
