package com.geneaazul.gedcomanalyzer.model;

import java.util.List;
import java.util.Optional;

import lombok.Getter;

@Getter
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class EnrichedSpouseWithChildren {

    private final Optional<EnrichedPerson> spouse;
    private final List<EnrichedPersonWithReference> childrenWithReference;
    private final List<EnrichedPerson> children;
    private final boolean isSeparated;
    private final Optional<Date> dateOfPartners;
    private final Optional<Date> dateOfSeparation;
    private final Optional<Place> placeOfPartners;
    private final Optional<Place> placeOfSeparation;

    private EnrichedSpouseWithChildren(
            Optional<EnrichedPerson> spouse,
            List<EnrichedPersonWithReference> childrenWithReference,
            boolean isSeparated,
            Optional<Date> dateOfPartners,
            Optional<Date> dateOfSeparation,
            Optional<Place> placeOfPartners,
            Optional<Place> placeOfSeparation) {
        this.spouse = spouse;
        this.childrenWithReference = childrenWithReference;
        this.children = childrenWithReference
                .stream()
                .map(EnrichedPersonWithReference::person)
                .toList();
        this.isSeparated = isSeparated;
        this.dateOfPartners = dateOfPartners;
        this.dateOfSeparation = dateOfSeparation;
        this.placeOfPartners = placeOfPartners;
        this.placeOfSeparation = placeOfSeparation;
    }

    public static EnrichedSpouseWithChildren of(
            Optional<EnrichedPerson> spouse,
            List<EnrichedPersonWithReference> childrenWithReference,
            boolean isSeparated,
            Optional<Date> dateOfPartners,
            Optional<Date> dateOfSeparation,
            Optional<Place> placeOfPartners,
            Optional<Place> placeOfSeparation) {
        return new EnrichedSpouseWithChildren(
                spouse,
                childrenWithReference,
                isSeparated,
                dateOfPartners,
                dateOfSeparation,
                placeOfPartners,
                placeOfSeparation);
    }

}
