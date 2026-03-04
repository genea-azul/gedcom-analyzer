package com.geneaazul.gedcomanalyzer.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DateTests {

    @Test
    void from_localDate_createsFullDate() {
        Date d = Date.from(LocalDate.of(1987, 7, 15));
        assertThat(d.isFullDate()).isTrue();
        assertThat(d.isPartialDate()).isFalse();
        assertThat(d.isOnlyYearDate()).isFalse();
        assertThat(d.getYear()).isEqualTo(Year.of(1987));
        assertThat(d.toLocalDate()).isEqualTo(LocalDate.of(1987, 7, 15));
        assertThat(d.format()).isEqualTo("15 JUL 1987");
    }

    @Test
    void parse_fullDateString_returnsDate() {
        assertThat(Date.parse("15 JUL 1987")).hasValueSatisfying(d -> {
            assertThat(d.isFullDate()).isTrue();
            assertThat(d.getYear().getValue()).isEqualTo(1987);
            assertThat(d.toLocalDate()).isEqualTo(LocalDate.of(1987, 7, 15));
        });
    }

    @Test
    void parse_yearOnly_returnsOnlyYearDate() {
        assertThat(Date.parse("1987")).hasValueSatisfying(d -> {
            assertThat(d.isOnlyYearDate()).isTrue();
            assertThat(d.isPartialDate()).isFalse();
            assertThat(d.isFullDate()).isFalse();
            assertThat(d.getYear().getValue()).isEqualTo(1987);
            assertThat(d.format()).isEqualTo("1987");
        });
    }

    @Test
    void parse_monthYear_returnsPartialDate() {
        assertThat(Date.parse("JUL 1987")).hasValueSatisfying(d -> {
            assertThat(d.isPartialDate()).isTrue();
            assertThat(d.isOnlyYearDate()).isFalse();
            assertThat(d.getYear().getValue()).isEqualTo(1987);
            assertThat(d.toYearMonth()).isEqualTo(java.time.YearMonth.of(1987, 7));
        });
    }

    @Test
    void parse_withOperator_includesInFormat() {
        assertThat(Date.parse("ABT 1987")).hasValueSatisfying(d -> {
            assertThat(d.format()).contains("ABT");
        });
    }

    @Test
    void parse_invalid_returnsEmpty() {
        assertThat(Date.parse("not a date")).isEmpty();
        assertThat(Date.parse("")).isEmpty();
    }

    @Test
    void compareTo_ordersByYearThenMonthThenDay() {
        Date d1 = Date.from(LocalDate.of(1987, 7, 15));
        Date d2 = Date.from(LocalDate.of(1988, 1, 1));
        Date d3 = Date.from(LocalDate.of(1987, 8, 1));
        assertThat(d1.compareTo(d2)).isLessThan(0);
        assertThat(d2.compareTo(d1)).isGreaterThan(0);
        assertThat(d1.compareTo(d3)).isLessThan(0);
    }

    @Test
    void isBefore_fullDates_comparesByDay() {
        Date d1 = Date.from(LocalDate.of(1987, 7, 15));
        Date d2 = Date.from(LocalDate.of(1987, 7, 20));
        assertThat(d1.isBefore(d2)).isTrue();
        assertThat(d2.isBefore(d1)).isFalse();
    }

    @Test
    void isCloseToByDay_withinDelta_returnsTrue() {
        Date d1 = Date.from(LocalDate.of(1987, 7, 15));
        Date d2 = Date.from(LocalDate.of(1987, 7, 17));
        assertThat(d1.isCloseToByDay(d2, Period.ofDays(5))).isTrue();
        assertThat(d1.isCloseToByDay(d2, Period.ofDays(1))).isFalse();
    }

    @Test
    void isCloseToByDay_onlyFullDates_returnsFalseForPartial() {
        Date full = Date.parse("15 JUL 1987").orElseThrow();
        Date partial = Date.parse("JUL 1987").orElseThrow();
        assertThat(full.isCloseToByDay(partial, Period.ofDays(0))).isFalse();
    }

    @Test
    void isCloseToByMonth_partialDates_withinDelta_returnsTrue() {
        Date d1 = Date.parse("JUL 1987").orElseThrow();
        Date d2 = Date.parse("AUG 1987").orElseThrow();
        assertThat(d1.isCloseToByMonth(d2, Period.ofMonths(2))).isTrue();
    }

    @Test
    void isCloseToByYear_onlyYearDates_withinDelta_returnsTrue() {
        Date d1 = Date.parse("1987").orElseThrow();
        Date d2 = Date.parse("1989").orElseThrow();
        assertThat(d1.isCloseToByYear(d2, Period.ofYears(3))).isTrue();
        assertThat(d1.isCloseToByYear(d2, Period.ofYears(1))).isFalse();
    }

    @Test
    void now_returnsCurrentDateInZone() {
        Date d = Date.now(ZoneId.of("UTC"));
        assertThat(d.getYear().getValue()).isEqualTo(LocalDate.now(ZoneId.of("UTC")).getYear());
    }
}
