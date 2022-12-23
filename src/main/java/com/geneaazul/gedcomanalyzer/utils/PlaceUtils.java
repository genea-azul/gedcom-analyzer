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

}
