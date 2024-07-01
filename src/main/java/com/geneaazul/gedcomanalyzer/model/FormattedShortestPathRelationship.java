package com.geneaazul.gedcomanalyzer.model;

import jakarta.annotation.Nullable;

public record FormattedShortestPathRelationship(
        String displayName,
        @Nullable String personInfo,
        @Nullable String relationshipDesc) {

}
