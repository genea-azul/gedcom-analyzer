package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.utils.PlaceUtils;

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
    private final Optional<String> placeOfPartners;
    private final Optional<String> placeOfSeparation;

    // Search values
    private final Optional<String> countryOfPartners;
    private final Optional<String> countryOfSeparation;

    private EnrichedSpouseWithChildren(
            Optional<EnrichedPerson> spouse,
            List<EnrichedPersonWithReference> childrenWithReference,
            boolean isSeparated,
            Optional<Date> dateOfPartners,
            Optional<Date> dateOfSeparation,
            Optional<String> placeOfPartners,
            Optional<String> placeOfSeparation) {
        this.spouse = spouse;
        this.childrenWithReference = childrenWithReference;
        this.children = childrenWithReference
                .stream()
                .map(EnrichedPersonWithReference::getPerson)
                .toList();
        this.isSeparated = isSeparated;
        this.dateOfPartners = dateOfPartners;
        this.dateOfSeparation = dateOfSeparation;
        this.placeOfPartners = placeOfPartners;
        this.placeOfSeparation = placeOfSeparation;

        this.countryOfPartners = placeOfPartners
                .map(PlaceUtils::removeLastParenthesis)
                .map(PlaceUtils::getCountry);
        this.countryOfSeparation = placeOfSeparation
                .map(PlaceUtils::removeLastParenthesis)
                .map(PlaceUtils::getCountry);
    }

    public static EnrichedSpouseWithChildren of(
            Optional<EnrichedPerson> spouse,
            List<EnrichedPersonWithReference> childrenWithReference,
            boolean isSeparated,
            Optional<Date> dateOfPartners,
            Optional<Date> dateOfSeparation,
            Optional<String> placeOfPartners,
            Optional<String> placeOfSeparation) {
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
