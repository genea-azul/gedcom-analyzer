package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.utils.PlaceUtils;

import java.util.Map;

public record Place(String name, String forSearch, String country) {

    public static Place of(String name, Map<String, Place> placesIndex) {
        return placesIndex.computeIfAbsent(name, k -> {
            String forSearch = PlaceUtils.removeLastParenthesis(k);
            String country = PlaceUtils.getCountry(forSearch);
            return new Place(k, forSearch, country);
        });
    }

}
