package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.utils.NameUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import java.util.Optional;

import jakarta.annotation.Nullable;

public record GivenNameAndSurname(
        @Nullable GivenName givenName,
        @Nullable Surname surname,
        @Nullable String simplifiedAka) {

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
        return new GivenNameAndSurname(
                normalizedGivenName.orElse(null),
                shortenedSurname.orElse(null),
                NameUtils.simplifyName(aka));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static GivenNameAndSurname of(
            Optional<GivenName> givenName,
            Optional<Surname> surname,
            Optional<String> aka) {
        return new GivenNameAndSurname(
                givenName.orElse(null),
                surname.orElse(null),
                aka
                        .map(NameUtils::simplifyName)
                        .orElse(null));
    }

    public boolean matches(@Nullable GivenNameAndSurname other) {
        if (other == null) {
            return false;
        }
        if (this.isAnyValueEmpty() || other.isAnyValueEmpty()) {
            return false;
        }
        if (this.surname.matches(other.surname)
                && this.givenName.matches(other.givenName)) {
            return true;
        }
        if (this.simplifiedAka != null) {
            String simplifiedGivenName = NameUtils.simplifyName(other.givenName.value());
            String simplifiedSurname = NameUtils.simplifyName(other.surname.value());
            if (this.simplifiedAka.contains(simplifiedGivenName) && this.simplifiedAka.contains(simplifiedSurname)) {
                return true;
            }
        }
        if (other.simplifiedAka != null) {
            String simplifiedGivenName = NameUtils.simplifyName(this.givenName.value());
            String simplifiedSurname = NameUtils.simplifyName(this.surname.value());
            if (other.simplifiedAka.contains(simplifiedGivenName) && other.simplifiedAka.contains(simplifiedSurname)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesWithOptionalGivenName(@Nullable GivenNameAndSurname other) {
        if (other == null) {
            return false;
        }
        if (!this.isGivenNameEmpty() && !other.isGivenNameEmpty()) {
            return false; // cause it is not optional given name
        }
        if (this.isSurnameEmpty() || other.isSurnameEmpty()) {
            return false;
        }
        return this.surname.matches(other.surname);
    }

}
