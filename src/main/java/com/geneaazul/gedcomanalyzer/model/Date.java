package com.geneaazul.gedcomanalyzer.model;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class Date implements Comparable<Date> {

    private static final String LOCAL_DATE_REGEX = " ?(ABT|EST|BEF|AFT)?(?:(?: ?(\\d{1,2}))? ?([A-Za-z]{3}))? ?(\\d{4})";
    private static final Pattern DATE_REGEX = Pattern.compile("(BET|FROM)?" + LOCAL_DATE_REGEX + "(?: ?(AND|TO)" + LOCAL_DATE_REGEX + ")?");

    private static final Map<String, String> MONTHS_MAPPING = Map.ofEntries(
            Map.entry("JAN", Month.JANUARY.name()),
            Map.entry("FEB", Month.FEBRUARY.name()),
            Map.entry("MAR", Month.MARCH.name()),
            Map.entry("APR", Month.APRIL.name()),
            Map.entry("MAY", Month.MAY.name()),
            Map.entry("JUN", Month.JUNE.name()),
            Map.entry("JUL", Month.JULY.name()),
            Map.entry("AUG", Month.AUGUST.name()),
            Map.entry("SEP", Month.SEPTEMBER.name()),
            Map.entry("OCT", Month.OCTOBER.name()),
            Map.entry("NOV", Month.NOVEMBER.name()),
            Map.entry("DEC", Month.DECEMBER.name())
    );

    public enum Operator {
        ABT, EST, BEF, AFT
    }

    private final Year year;
    private final Month month;
    private final Integer day;
    private final Operator operator;
    private final Date secondary;

    @Getter(AccessLevel.NONE)
    // Only set when day is not null
    private final LocalDate localDate;

    @Getter(AccessLevel.NONE)
    // Only set when month is not null
    private final YearMonth yearMonth;

    private Date(Year year, @Nullable Month month, @Nullable Integer day, @Nullable Operator operator, @Nullable Date secondary) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.operator = operator;
        this.secondary = secondary;
        this.localDate = day != null && month != null ? LocalDate.of(year.getValue(), month, day) : null;
        this.yearMonth = month != null ? YearMonth.of(year.getValue(), month) : null;
    }

    private Date(LocalDate localDate) {
        this.year = Year.of(localDate.getYear());
        this.month = localDate.getMonth();
        this.day = localDate.getDayOfMonth();
        this.operator = null;
        this.secondary = null;
        this.localDate = localDate;
        this.yearMonth = YearMonth.of(year.getValue(), month);
    }

    public static Date now(ZoneId zoneId) {
        return new Date(LocalDate.now(zoneId));
    }

    public boolean isFullDate() {
        return day != null;
    }

    public boolean isPartialDate() {
        return day == null && month != null;
    }

    public boolean isOnlyYearDate() {
        return day == null && month == null;
    }

    public LocalDate toLocalDate() {
        return localDate;
    }

    public YearMonth toYearMonth() {
        return yearMonth;
    }

    public boolean isBefore(LocalDate date) {
        if (day != null) {
            return toLocalDate().isBefore(date);
        }
        if (month != null) {
            return !LocalDate.of(year.getValue(), month, 1).isAfter(date);
        }
        return !LocalDate.of(year.getValue(), 1, 1).isAfter(date);
    }

    public boolean isCloseToByDay(Date other, Period delta) {
        if (!this.isFullDate() || !other.isFullDate()) {
            return false;
        }
        LocalDate ym1 = this.toLocalDate();
        LocalDate ym2 = other.toLocalDate();
        if (delta.isZero()) {
            return ym1.isEqual(ym2);
        }
        if (ym1.isAfter(ym2)) {
            return ChronoUnit.DAYS.between(ym2, ym1) <= delta.getDays();
        } else {
            return ChronoUnit.DAYS.between(ym1, ym2) <= delta.getDays();
        }
    }

    public boolean isCloseToByMonth(Date other, Period delta) {
        // If both days are set, then it is a "by day" comparison.. so at least one must be partial
        if (!this.isPartialDate() && !other.isPartialDate()) {
            return false;
        }
        if (this.isOnlyYearDate() || other.isOnlyYearDate()) {
            return false;
        }
        YearMonth ym1 = this.toYearMonth();
        YearMonth ym2 = other.toYearMonth();
        if (delta.isZero()) {
            return ym1.equals(ym2);
        }
        if (ym1.isAfter(ym2)) {
            return ChronoUnit.MONTHS.between(ym2, ym1) <= delta.getMonths();
        } else {
            return ChronoUnit.MONTHS.between(ym1, ym2) <= delta.getMonths();
        }
    }

    public boolean isCloseToByYear(Date other, Period delta) {
        // If both days or months are set, then it is a "by day" or "by month" comparison
        if (!this.isOnlyYearDate() && !other.isOnlyYearDate()) {
            return false;
        }
        if (delta.isZero()) {
            return this.year.equals(other.year);
        }
        if (this.year.isAfter(other.year)) {
            return ChronoUnit.YEARS.between(other.year, this.year) <= delta.getYears();
        } else {
            return ChronoUnit.YEARS.between(this.year, other.year) <= delta.getYears();
        }
    }

    public String format() {
        return (secondary == null ? "" : "BET ")
                + (operator == null ? "" : operator + " ")
                + (day == null ? "" : day + " ")
                + (month == null ? "" : month.toString().substring(0, 3) + " ")
                + year
                + (secondary == null ? "" : " AND " + secondary);
    }

    @Override
    public String toString() {
        return format();
    }

    public static Optional<Date> parse(String dateStr) {
        Matcher matcher = DATE_REGEX.matcher(dateStr);

        if (matcher.matches()) {
            Optional<String> operator = trimToNull(matcher, 2);
            Optional<String> day = trimToNull(matcher, 3);
            Optional<String> month = trimToNull(matcher, 4);
            Optional<String> year = trimToNull(matcher, 5);
            Optional<String> secKey = trimToNull(matcher, 6);
            Optional<String> secOperator = trimToNull(matcher, 7);
            Optional<String> secDay = trimToNull(matcher, 8);
            Optional<String> secMonth = trimToNull(matcher, 9);
            Optional<String> secYear = trimToNull(matcher, 10);

            Optional<Date> secondary = secKey
                    .map(key -> new Date(
                            secYear
                                    .map(Year::parse)
                                    .orElse(null),
                            secMonth
                                    .map(String::toUpperCase)
                                    .map(MONTHS_MAPPING::get)
                                    .map(Month::valueOf)
                                    .orElse(null),
                            secDay
                                    .map(Integer::parseInt)
                                    .orElse(null),
                            secOperator
                                    .map(Operator::valueOf)
                                    .orElse(null),
                            null));

            return Optional.of(
                    new Date(
                            year
                                    .map(Year::parse)
                                    .orElse(null),
                            month
                                    .map(String::toUpperCase)
                                    .map(MONTHS_MAPPING::get)
                                    .map(Month::valueOf)
                                    .orElse(null),
                            day
                                    .map(Integer::parseInt)
                                    .orElse(null),
                            operator
                                    .map(Operator::valueOf)
                                    .orElse(null),
                            secondary
                                    .orElse(null)));
        }

        log.warn("Date not parsed: {}", dateStr);
        return Optional.empty();
    }

    @Override
    public int compareTo(Date other) {
        int cmp = ObjectUtils.compare(this.year, other.year, true);
        if (cmp != 0) {
            return cmp;
        }
        cmp = ObjectUtils.compare(this.month, other.month, true);
        if (cmp != 0) {
            return cmp;
        }
        return ObjectUtils.compare(this.day, other.day, true);
    }

    private static Optional<String> trimToNull(Matcher matcher, int group) {
        return Optional.ofNullable(StringUtils.trimToNull(matcher.group(group)));
    }

}
