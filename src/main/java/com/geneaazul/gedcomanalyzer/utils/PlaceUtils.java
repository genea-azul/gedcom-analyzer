package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlaceUtils {

    /**
     * i.e: Potenza, Italia -> Italia
     */
    public static String getCountry(String place) {
        int pos = place.lastIndexOf(",");
        return pos != -1 ? place.substring(pos + 1).trim() : place;
    }

    /**
     * i.e: Italia (antes Suiza) -> Italia
     */
    public static String removeLastParenthesis(String place) {
        return place.endsWith(")") ? StringUtils.substringBeforeLast(place, "(").trim() : place;
    }

    /**
     * i.e: Inglaterra, Reino Unido -> Inglaterra
     */
    public static String adjustPlace(String place) {
        if (place == null) {
            return null;
        }
        if (place.endsWith("Reino Unido")) {
            return StringUtils.substringBeforeLast(place, ",");
        }
        return place;
    }

    /**
     * i.e: Estados Unidos -> EU.
     */
    public static String adjustCountryForReport(String country) {
        if (country == null) {
            return null;
        }
        if (country.startsWith("Estados Unidos")) {
            return "EU.";
        }
        if (country.startsWith("Países Bajos")) {
            return "PB.";
        }
        if (country.startsWith("República Dominicana")) {
            return "RD.";
        }
        if (country.startsWith("Reino Unido")) {
            return "RU.";
        }
        if (country.startsWith("Océano Atlántico")) {
            return "OA.";
        }
        return country;
    }

}
