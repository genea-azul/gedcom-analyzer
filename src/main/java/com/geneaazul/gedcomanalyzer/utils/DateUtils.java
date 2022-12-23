package com.geneaazul.gedcomanalyzer.utils;

import java.time.Month;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DateUtils {

    public enum AstrologicalSign {
        ARIES, TAURUS, GEMINI, CANCER, LEO, VIRGO, LIBRA, SCORPIO, SAGITTARIUS, CAPRICORN, AQUARIUS, PISCES
    }

    public static AstrologicalSign getAstrologicalSign(Month month, int day, boolean useMinDayOnOverlap) {
        return switch (month) {
            case JANUARY -> (day < (useMinDayOnOverlap ? 20 : 21)) ? AstrologicalSign.CAPRICORN : AstrologicalSign.AQUARIUS;
            case FEBRUARY -> (day < (useMinDayOnOverlap ? 19 : 20)) ? AstrologicalSign.AQUARIUS : AstrologicalSign.PISCES;
            case MARCH -> (day < (useMinDayOnOverlap ? 20 : 21)) ? AstrologicalSign.PISCES : AstrologicalSign.ARIES;
            case APRIL -> (day < (useMinDayOnOverlap ? 20 : 21)) ? AstrologicalSign.ARIES : AstrologicalSign.TAURUS;
            case MAY -> (day < (useMinDayOnOverlap ? 21 : 22)) ? AstrologicalSign.TAURUS : AstrologicalSign.GEMINI;
            case JUNE -> (day < (useMinDayOnOverlap ? 21 : 22)) ? AstrologicalSign.GEMINI : AstrologicalSign.CANCER;
            case JULY -> (day < (useMinDayOnOverlap ? 23 : 24)) ? AstrologicalSign.CANCER : AstrologicalSign.LEO;
            case AUGUST -> (day < (useMinDayOnOverlap ? 23 : 24)) ? AstrologicalSign.LEO : AstrologicalSign.VIRGO;
            case SEPTEMBER -> (day < (useMinDayOnOverlap ? 23 : 24)) ? AstrologicalSign.VIRGO : AstrologicalSign.LIBRA;
            case OCTOBER -> (day < (useMinDayOnOverlap ? 23 : 24)) ? AstrologicalSign.LIBRA : AstrologicalSign.SCORPIO;
            case NOVEMBER -> (day < (useMinDayOnOverlap ? 22 : 23)) ? AstrologicalSign.SCORPIO : AstrologicalSign.SAGITTARIUS;
            case DECEMBER -> (day < (useMinDayOnOverlap ? 21 : 22)) ? AstrologicalSign.SAGITTARIUS : AstrologicalSign.CAPRICORN;
        };
    }

}
