package com.geneaazul.gedcomanalyzer.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceUtilsTests {

    @Test
    void getCountry_returnsNullWhenPlaceIsNull() {
        assertThat(PlaceUtils.getCountry(null)).isNull();
    }

    @Test
    void getCountry_returnsLastComponentAfterLastComma() {
        assertThat(PlaceUtils.getCountry("Trento, Trentino-Alto Adige, Italia (antes Austria)"))
                .isEqualTo("Italia (antes Austria)");
    }

    @Test
    void getCountry_returnsTrimmedPlaceWhenNoComma() {
        assertThat(PlaceUtils.getCountry("Italia")).isEqualTo("Italia");
    }

    @Test
    void removeLastParenthesis_removesLastParenthesisContent() {
        assertThat(PlaceUtils.removeLastParenthesis("Trento, Trentino-Alto Adige, Italia (antes Austria)"))
                .isEqualTo("Trento, Trentino-Alto Adige, Italia");
    }

    @Test
    void removeLastParenthesis_returnsSameWhenNotEndingWithParen() {
        assertThat(PlaceUtils.removeLastParenthesis("Trento, Italia")).isEqualTo("Trento, Italia");
    }

    @Test
    void removeLastParenthesis_returnsNullWhenPlaceIsNull() {
        assertThat(PlaceUtils.removeLastParenthesis(null)).isNull();
    }

    @Test
    void adjustPlace_returnsNullWhenPlaceIsNull() {
        assertThat(PlaceUtils.adjustPlace(null)).isNull();
    }

    @Test
    void adjustPlace_removesReinoUnidoSuffix() {
        assertThat(PlaceUtils.adjustPlace("Inglaterra, Reino Unido")).isEqualTo("Inglaterra");
    }

    @Test
    void adjustPlace_replacesFrenchPlaces() {
        assertThat(PlaceUtils.adjustPlace("Haute-Garonne, Occitania, Francia"))
                .isEqualTo("Alto Garona, Mediodía-Pirineos, Francia");
    }

    @Test
    void adjustPlace_fixesFriuliVeneziaGiulia() {
        assertThat(PlaceUtils.adjustPlace("X, Friuli Venezia Giulia, Italia"))
                .isEqualTo("X, Friuli-Venezia Giulia, Italia");
    }

    @Test
    void adjustPlace_returnsSameWhenNoMatch() {
        assertThat(PlaceUtils.adjustPlace("Milano, Italia")).isEqualTo("Milano, Italia");
    }

    @Test
    void adjustCountryForReport_returnsNullWhenCountryIsNull() {
        assertThat(PlaceUtils.adjustCountryForReport(null)).isNull();
    }

    @Test
    void adjustCountryForReport_abbreviatesEstadosUnidos() {
        assertThat(PlaceUtils.adjustCountryForReport("Estados Unidos")).isEqualTo("EU.");
    }

    @Test
    void adjustCountryForReport_abbreviatesPaisesBajos() {
        assertThat(PlaceUtils.adjustCountryForReport("Países Bajos")).isEqualTo("PB.");
    }

    @Test
    void adjustCountryForReport_returnsSameWhenNoMatch() {
        assertThat(PlaceUtils.adjustCountryForReport("Italia")).isEqualTo("Italia");
    }

    @Test
    void removeSubCityComponent_removesSubCityPrefix() {
        assertThat(PlaceUtils.removeSubCityComponent("Parroquia San X, Ciudad, País"))
                .isEqualTo("Ciudad, País");
    }

    @Test
    void removeSubCityComponent_keepsExceptionPrefix() {
        assertThat(PlaceUtils.removeSubCityComponent("Capilla del Monte, Ciudad, País"))
                .isEqualTo("Capilla del Monte, Ciudad, País");
    }

    @Test
    void removeSubCityComponent_returnsPlaceWhenNoComma() {
        assertThat(PlaceUtils.removeSubCityComponent("Solo Lugar")).isEqualTo("Solo Lugar");
    }

    @Test
    void reversePlaceWords_reversesComponents() {
        String[] reversed = PlaceUtils.reversePlaceWords("City, Region, Country");
        assertThat(reversed).containsExactly("Country", "Region", "City");
    }

    @Test
    void reversePlaceWords_returnsNullWhenPlaceIsNull() {
        assertThat(PlaceUtils.reversePlaceWords(null)).isNull();
    }

    @Test
    void sortPlaces_ordersByCountryThenComponents() {
        List<String> sorted = PlaceUtils.sortPlaces(List.of("B City, B Country", "A City, A Country"), false);
        assertThat(sorted).containsExactly("A City, A Country", "B City, B Country");
    }

    @Test
    void sortPlaces_keepPlaceReversed_keepsReversedOrder() {
        List<String> sorted = PlaceUtils.sortPlaces(List.of("City, Country"), true);
        assertThat(sorted).containsExactly("Country, City");
    }

    @Test
    void reversedPlaceArrayComparator_comparesByCountryFirst() {
        String[] a = new String[]{"Italia", "Roma"};
        String[] b = new String[]{"España", "Madrid"};
        assertThat(PlaceUtils.REVERSED_PLACE_ARRAY_COMPARATOR.compare(a, b)).isGreaterThan(0);
    }

    @Test
    void reversedPlaceArrayComparator_thenByLength() {
        String[] a = new String[]{"Italia", "Roma"};
        String[] b = new String[]{"Italia"};
        assertThat(PlaceUtils.REVERSED_PLACE_ARRAY_COMPARATOR.compare(a, b)).isGreaterThan(0);
    }
}
