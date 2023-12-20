package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import java.util.Optional;

import jakarta.annotation.Nullable;

public record GivenNameAndSurname(
        @Nullable GivenName givenName,
        @Nullable Surname surname,
        @Nullable Aka aka) {

    public boolean isAnyValueEmpty() {
        return givenName == null || surname == null;
    }

    public boolean isGivenNameEmpty() {
        return givenName == null;
    }

    public boolean isSurnameEmpty() {
        return surname == null;
    }

    public boolean areAllValuesNotEmpty() {
        return givenName != null && surname != null;
    }

    public static GivenNameAndSurname of(
            @Nullable String givenName,
            @Nullable String surname,
            @Nullable SexType sex,
            GedcomAnalyzerProperties properties) {
        return of(givenName, surname, null, sex, properties);
    }

    public static GivenNameAndSurname of(
            @Nullable String givenName,
            @Nullable String surname,
            @Nullable String aka,
            @Nullable SexType sex,
            GedcomAnalyzerProperties properties) {
        Optional<GivenName> normalizedGivenName = PersonUtils.getNormalizedGivenName(givenName, sex, properties.getNormalizedGivenNamesMap());
        Optional<Surname> shortenedSurname = PersonUtils.getShortenedSurnameMainWord(surname, properties.getNormalizedSurnamesMap());
        Optional<Aka> simplifiedAka = PersonUtils.getSimplifiedAka(aka, sex, properties.getNormalizedGivenNamesMap());
        return new GivenNameAndSurname(
                normalizedGivenName.orElse(null),
                shortenedSurname.orElse(null),
                simplifiedAka.orElse(null));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static GivenNameAndSurname of(
            Optional<GivenName> givenName,
            Optional<Surname> surname,
            Optional<Aka> aka) {
        return new GivenNameAndSurname(
                givenName.orElse(null),
                surname.orElse(null),
                aka.orElse(null));
    }

    public boolean matches(@Nullable GivenNameAndSurname other) {
        if (other == null) {
            return false;
        }
        if (this.isAnyValueEmpty() || other.isAnyValueEmpty()) {
            return false;
        }
        boolean matchesSurname = this.surname.matches(other.surname);
        if (matchesSurname && this.givenName.matches(other.givenName)) {
            return true;
        }
        if (this.aka != null) {
            if (matchesSurname
                    && this.aka.tentativeGivenName()
                            .map(tentative -> tentative.matches(other.givenName))
                            .orElse(false)) {
                return true;
            }
            if (this.aka.simplified().contains(other.givenName.simplified())
                    && this.aka.simplified().contains(other.surname.simplified())) {
                return true;
            }
        }
        if (other.aka != null) {
            if (matchesSurname
                    && other.aka.tentativeGivenName()
                            .map(tentative -> tentative.matches(this.givenName))
                            .orElse(false)) {
                return true;
            }
            if (other.aka.simplified().contains(this.givenName.simplified())
                    && other.aka.simplified().contains(this.surname.simplified())) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesSurnameWhenMissingAnyGivenName(@Nullable GivenNameAndSurname other) {
        if (other == null) {
            return false;
        }
        if (!this.isGivenNameEmpty() && !other.isGivenNameEmpty()) {
            return false; // cause it is not missing any given name
        }
        if (this.isSurnameEmpty() || other.isSurnameEmpty()) {
            return false;
        }
        return this.surname.matches(other.surname);
    }

}
