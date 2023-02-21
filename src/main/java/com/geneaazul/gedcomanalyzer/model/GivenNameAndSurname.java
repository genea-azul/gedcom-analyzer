package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import java.util.Optional;

import jakarta.annotation.Nullable;

public record GivenNameAndSurname(@Nullable GivenName givenName, @Nullable Surname surname) {

    public boolean isAnyValueEmpty() {
        return givenName == null || surname == null;
    }

    public boolean areAllValuesNotEmpty() {
        return givenName != null && surname != null;
    }

    public static GivenNameAndSurname of(
            @Nullable String givenName,
            @Nullable String surname,
            @Nullable SexType sex,
            GedcomAnalyzerProperties properties) {
        Optional<GivenName> normalizedGivenName = PersonUtils.getNormalizedGivenName(givenName, sex, properties.getNormalizedGivenNamesMap());
        Optional<Surname> shortenedSurname = PersonUtils.getShortenedSurnameMainWord(surname, properties.getNormalizedSurnamesMap());
        return new GivenNameAndSurname(
                normalizedGivenName.orElse(null),
                shortenedSurname.orElse(null));
    }

    public static GivenNameAndSurname of(
            @Nullable GivenName givenName,
            @Nullable Surname surname) {
        return new GivenNameAndSurname(givenName, surname);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static GivenNameAndSurname of(
            Optional<GivenName> givenName,
            Optional<Surname> surname) {
        return new GivenNameAndSurname(givenName.orElse(null), surname.orElse(null));
    }

    public boolean matches(GivenNameAndSurname other) {
        if (other == null) {
            return false;
        }
        if (isAnyValueEmpty()) {
            return false;
        }
        return this.surname.matches(other.surname)
                && this.givenName.matches(other.givenName);
    }

}
