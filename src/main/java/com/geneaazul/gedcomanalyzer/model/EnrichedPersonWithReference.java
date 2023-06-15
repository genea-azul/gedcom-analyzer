package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;

import java.util.Optional;

public record EnrichedPersonWithReference(
        EnrichedPerson person,
        Optional<ReferenceType> referenceType) {

}
