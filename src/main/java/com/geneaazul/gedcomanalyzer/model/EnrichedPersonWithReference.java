package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;

import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class EnrichedPersonWithReference {

    private final EnrichedPerson person;
    private final Optional<ReferenceType> referenceType;

    public static EnrichedPersonWithReference of(EnrichedPerson person, Optional<ReferenceType> referenceType) {
        return new EnrichedPersonWithReference(person, referenceType);
    }

}
