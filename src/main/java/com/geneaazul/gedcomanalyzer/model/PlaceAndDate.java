package com.geneaazul.gedcomanalyzer.model;

import java.util.Optional;

public record PlaceAndDate(Place place, Date date) {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Optional<PlaceAndDate> ofPlaceOrEmpty(Optional<Place> place, Optional<Date> date) {
        if (place.isEmpty() && date.isEmpty()) {
            return Optional.empty();
        }
        return place
                .map(p -> new PlaceAndDate(p, date.orElse(null)));
    }

}
