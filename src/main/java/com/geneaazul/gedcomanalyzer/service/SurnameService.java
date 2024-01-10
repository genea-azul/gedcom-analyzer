package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnameResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.NameUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;
import com.geneaazul.gedcomanalyzer.utils.StreamUtils;

import org.springframework.stereotype.Service;

import org.apache.commons.lang3.StringUtils;

import java.time.Year;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SurnameService {

    private final GedcomHolder gedcomHolder;

    public SearchSurnamesResultDto search(SearchSurnamesDto searchSurnamesDto) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        Map<String, String> normalizedSurnamesMap = gedcom.getProperties().getNormalizedSurnamesMap();

        List<SearchSurnameResultDto> searchSurnameResults = searchSurnamesDto
                .getSurnames()
                .stream()
                .map(surname -> PersonUtils
                        .getShortenedSurnameMainWord(surname, normalizedSurnamesMap))
                .flatMap(Optional::stream)
                .filter(StreamUtils.distinctByKey(Surname::shortenedMainWord))
                .map(surname -> {
                    List<EnrichedPerson> persons = searchPersonsBySurname(surname, gedcom);

                    List<String> variants = getMatchingSurnameVariants(surname, persons, normalizedSurnamesMap);

                    List<String> countries = persons
                            .stream()
                            .map(EnrichedPerson::getPlacesOfAnyEvent)
                            .flatMap(List::stream)
                            .map(Place::country)
                            .distinct()
                            .sorted()
                            .toList();

                    IntSummaryStatistics yearsStats = persons
                            .stream()
                            .flatMap(person -> Stream.concat(
                                    Stream
                                            .of(
                                                    person.getDateOfBirth(),
                                                    person.getDateOfDeath())
                                            .flatMap(Optional::stream),
                                    person
                                            .getSpousesWithChildren()
                                            .stream()
                                            .flatMap(spouseWithChildren -> Stream
                                                    .of(
                                                            spouseWithChildren.getDateOfPartners(),
                                                            spouseWithChildren.getDateOfSeparation()))
                                            .flatMap(Optional::stream)))
                            .map(Date::getYear)
                            .collect(Collectors.summarizingInt(Year::getValue));

                    return SearchSurnameResultDto.builder()
                            .surname(surname.value())
                            .frequency(persons.size())
                            .variants(variants)
                            .countries(countries)
                            .firstSeenYear(Optional.of(yearsStats.getMin())
                                    .filter(this::isValidMinMax)
                                    .orElse(null))
                            .lastSeenYear(Optional.of(yearsStats.getMax())
                                    .filter(this::isValidMinMax)
                                    .orElse(null))
                            .build();
                })
                .toList();

        return SearchSurnamesResultDto.builder()
                .surnames(searchSurnameResults)
                .build();
    }

    private List<String> getMatchingSurnameVariants(
            Surname surname,
            List<EnrichedPerson> persons,
            Map<String, String> normalizedSurnamesMap) {
        return persons
                .stream()
                .map(EnrichedPerson::getSurname)
                .flatMap(Optional::stream)
                .distinct()
                .filter(surname::matches)
                // Try to suppress compound surnames
                .map(variant -> suppressCompoundSurname(variant, normalizedSurnamesMap))
                // Skip same surname
                .filter(variant -> !StringUtils.equalsIgnoreCase(variant, surname.value()))
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getSurnameVariants(
            Surname surname,
            List<String> variantCandidates,
            Map<String, String> normalizedSurnamesMap) {
        return variantCandidates
                .stream()
                .distinct()
                .map(variant -> PersonUtils.getShortenedSurnameMainWord(variant, normalizedSurnamesMap))
                .flatMap(Optional::stream)
                // Try to suppress compound surnames
                .map(variant -> suppressCompoundSurname(variant, normalizedSurnamesMap))
                // Skip same surname
                .filter(variant -> !StringUtils.equalsIgnoreCase(variant, surname.value()))
                .distinct()
                .sorted()
                .toList();
    }

    private String suppressCompoundSurname(
            Surname surname,
            Map<String, String> normalizedSurnamesMap) {

        if (!surname.value().contains(" ")) {
            return surname.value();
        }

        String value = surname.value();
        String simplifed = surname.simplified();

        while (simplifed.contains(" ")) {

            simplifed = StringUtils.substringBeforeLast(simplifed, " ");
            String normalized = NameUtils.normalizeSurnameToMainWord(simplifed, normalizedSurnamesMap);

            if (!surname.normalizedMainWord().equals(normalized)) {
                break;
            }

            value = StringUtils.substringBeforeLast(value, " ");
        }

        return value;
    }

    private List<EnrichedPerson> searchPersonsBySurname(
            Surname surname,
            EnrichedGedcom gedcom) {

        return Arrays.stream(SexType.values())
                .map(sex -> gedcom.getPersonsBySurnameMainWordAndSex(surname, sex))
                .flatMap(List::stream)
                .toList();
    }

    private boolean isValidMinMax(Integer integer) {
        return integer != Integer.MIN_VALUE && integer != Integer.MAX_VALUE;
    }

}
