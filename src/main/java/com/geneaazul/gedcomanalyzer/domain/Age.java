package com.geneaazul.gedcomanalyzer.domain;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import lombok.Getter;

@Getter
public class Age {

    private final Period period;

    private final ChronoUnit unit;

    private Age(Date dateOfBirth, Date dateOfDeath) {
        if (dateOfBirth.isOnlyYearDate() || dateOfDeath.isOnlyYearDate()) {
            int years = (int) ChronoUnit.YEARS.between(dateOfBirth.getYear(), dateOfDeath.getYear());
            this.period = Period.ofYears(years);
            this.unit = ChronoUnit.YEARS;
        } else if (dateOfBirth.isPartialDate() || dateOfDeath.isPartialDate()) {
            int months = (int) ChronoUnit.MONTHS.between(dateOfBirth.toYearMonth(), dateOfDeath.toYearMonth());
            this.period = Period.ofMonths(months);
            this.unit = ChronoUnit.MONTHS;
        } else {
            this.period = Period.between(dateOfBirth.toLocalDate(), dateOfDeath.toLocalDate());
            this.unit = ChronoUnit.DAYS;
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Optional<Age> of(Optional<Date> dateOfBirth, Optional<Date> dateOfDeath) {
        return dateOfBirth.isPresent() && dateOfDeath.isPresent()
                ? Optional.of(new Age(dateOfBirth.get(), dateOfDeath.get()))
                : Optional.empty();
    }

    public boolean isExact() {
        return unit == ChronoUnit.DAYS;
    }

    public int getYears() {
        return period.getYears();
    }

}
