package com.geneaazul.gedcomanalyzer.model;

import java.time.Month;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public record Age(Period period, ChronoUnit unit) implements Comparable<Age> {

    private static Age of(Date dateOfBirth, Date dateOfDeath) {
        if (dateOfBirth.isOnlyYearDate() || dateOfDeath.isOnlyYearDate()) {
            int years = (int) ChronoUnit.YEARS.between(dateOfBirth.getYear(), dateOfDeath.getYear());
            if (dateOfBirth.isPartialDate() && dateOfBirth.getMonth().getValue() >= Month.JULY.getValue()
                    || dateOfDeath.isPartialDate() && dateOfDeath.getMonth().getValue() < Month.JULY.getValue()) {
                years--;
            }
            return new Age(Period.ofYears(years), ChronoUnit.YEARS);
        }
        if (dateOfBirth.isPartialDate() || dateOfDeath.isPartialDate()) {
            int totalMonths = (int) ChronoUnit.MONTHS.between(dateOfBirth.toYearMonth(), dateOfDeath.toYearMonth());
            if (dateOfBirth.isFullDate() && dateOfBirth.getDay() >= 15
                    || dateOfDeath.isFullDate() && dateOfDeath.getDay() < 15) {
                totalMonths--;
            }
            int years = totalMonths / 12;
            int months = totalMonths % 12;
            return new Age(Period.of(years, months, 0), ChronoUnit.MONTHS);
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

    @Override
    public int compareTo(Age other) {
        int yearsCmp =  Integer.compare(this.period.getYears(), other.period.getYears());
        if (yearsCmp != 0) {
            return yearsCmp;
        }

        if (this.unit == ChronoUnit.YEARS || other.unit == ChronoUnit.YEARS) {
            if (this.unit == other.unit) {
                return 0;
            }
            return (this.unit != ChronoUnit.YEARS) ? 1 : -1;
        }

        int monthsCmp = Long.compare(this.period.getMonths(), other.period.getMonths());
        if (monthsCmp != 0) {
            return monthsCmp;
        }

        if (this.unit == ChronoUnit.MONTHS || other.unit == ChronoUnit.MONTHS) {
            if (this.unit == other.unit) {
                return 0;
            }
            return (this.unit != ChronoUnit.MONTHS) ? 1 : -1;
        }

        return Integer.compare(this.period.getDays(), other.period.getDays());
    }

}
