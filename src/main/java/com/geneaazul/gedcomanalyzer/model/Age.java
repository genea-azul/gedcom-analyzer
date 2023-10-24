package com.geneaazul.gedcomanalyzer.model;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public record Age(Period period, ChronoUnit unit) {

    private static Age of(Date dateOfBirth, Date dateOfDeath) {
        if (dateOfBirth.isOnlyYearDate() || dateOfDeath.isOnlyYearDate()) {
            int years = (int) ChronoUnit.YEARS.between(dateOfBirth.getYear(), dateOfDeath.getYear());
            return new Age(Period.ofYears(years), ChronoUnit.YEARS);
        }
        if (dateOfBirth.isPartialDate() || dateOfDeath.isPartialDate()) {
            int months = (int) ChronoUnit.MONTHS.between(dateOfBirth.toYearMonth(), dateOfDeath.toYearMonth());
            return new Age(Period.ofMonths(months), ChronoUnit.MONTHS);
        }
        Period days = Period.between(dateOfBirth.toLocalDate(), dateOfDeath.toLocalDate());
        return new Age(days, ChronoUnit.DAYS);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Optional<Age> of(Optional<Date> dateOfBirth, Optional<Date> dateOfDeath) {
        return dateOfBirth.isPresent() && dateOfDeath.isPresent()
                ? Optional.of(Age.of(dateOfBirth.get(), dateOfDeath.get()))
                : Optional.empty();
    }

    public static Age ofYears(int years) {
        return new Age(Period.ofYears(years), ChronoUnit.YEARS);
    }

    public boolean isExact() {
        return unit == ChronoUnit.DAYS;
    }

    public int getYears() {
        return period.getYears();
    }

}
