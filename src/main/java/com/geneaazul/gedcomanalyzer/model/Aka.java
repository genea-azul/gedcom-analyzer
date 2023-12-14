package com.geneaazul.gedcomanalyzer.model;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record Aka(
        String value,
        String simplified,
        Optional<GivenName> tentativeGivenName) {

    public static Aka of(String value, String simplified, Optional<GivenName> tentativeGivenName) {
        return new Aka(value, simplified, tentativeGivenName);
    }

}
