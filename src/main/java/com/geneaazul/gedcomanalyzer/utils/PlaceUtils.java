package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.Nullable;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlaceUtils {

    private static final String[] SEARCH_PLACE = new String[]{ "Pirineos-Atlánticos", ", Pau,", ", Oloron-Sainte-Marie," };
    private static final String[] REPLACEMENT_PLACE = new String[]{ "Pirineos Atlánticos", ",", "," };

    private static final String[] SUB_CITY_PLACE_PREFIXES = new String[] {
            "Basílica",
            "Capilla",
            "Catedral",
            "Cementerio",
            "Colegio",
            "Ermita",
            "Escuela",
            "Hospital",
            "Iglesia",
            "Parrocchia",
            "Parroquia",
            "Santuario",
            "Universidad"
    };

    private static final String[] SUB_CITY_PLACE_SUFFIXES = new String[] {
            "Hospital",
            "Medical Center"
    };

    private static final String[] SUB_CITY_PLACE_PREFIXES_EXCEPTIONS = new String[] {
            "Capilla del Monte",
            "Capilla del Señor"
    };

    /**
     * i.e:  Trento, Trentino-Alto Adige, Italia (antes Austria) -> Italia (antes Austria)
     */
    public static String getCountry(String place) {
        int pos = place.lastIndexOf(",");
        return pos != -1 ? place.substring(pos + 1).trim() : place;
    }

    /**
     * i.e:  Trento, Trentino-Alto Adige, Italia (antes Austria) ->  Trento, Trentino-Alto Adige, Italia
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
        if (place.endsWith("Francia")) {
            return StringUtils.replaceEach(place, SEARCH_PLACE, REPLACEMENT_PLACE);
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

    public static List<String> sortPlaces(Collection<String> places, boolean keepPlaceReversed) {
        return places
                .stream()
                .map(PlaceUtils::reversePlaceWords)
                .sorted(REVERSED_PLACE_ARRAY_COMPARATOR)
                .peek(place -> {
                    if (!keepPlaceReversed) {
                        ArrayUtils.reverse(place);
                    }
                })
                .map(place -> StringUtils.join(place, ", "))
                .toList();
    }

    public static String[] reversePlaceWords(@Nullable String place) {
        return Optional.ofNullable(place)
                .map(p -> StringUtils.splitByWholeSeparator(p, ", "))
                .map(array -> {
                    ArrayUtils.reverse(array);
                    return array;
                })
                .orElse(null);
    }

    public static String removeSubCityComponent(String place) {
        if (StringUtils.isEmpty(place) || !StringUtils.contains(place, ", ")) {
            return place;
        }
        if (StringUtils.startsWithAny(place, SUB_CITY_PLACE_PREFIXES)
                && !StringUtils.startsWithAny(place, SUB_CITY_PLACE_PREFIXES_EXCEPTIONS)) {
            return StringUtils.substringAfter(place, ", ");
        } else {
            String firstComponent = StringUtils.substringBefore(place, ", ");
            if (StringUtils.endsWithAny(firstComponent, SUB_CITY_PLACE_SUFFIXES)) {
                return StringUtils.substringAfter(place, ", ");
            }
        }
        return place;
    }

    public static final Comparator<String[]> REVERSED_PLACE_ARRAY_COMPARATOR = (a1, a2) -> {
        int cmp = 0;
        int i = 0;
        while (i < a1.length && i < a2.length) {
            if (i == 0) {
                String country1 = PlaceUtils.removeLastParenthesis(a1[0]);
                String country2 = PlaceUtils.removeLastParenthesis(a2[0]);
                if ((cmp = country1.compareTo(country2)) != 0) {
                    break;
                }
            } else if ((cmp = a1[i].compareTo(a2[i])) != 0) {
                break;
            }
            i++;
        }
        return cmp == 0 ? Integer.compare(a1.length, a2.length) : cmp;
    };

}
