package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnameResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
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
                .filter(distinctByKey(Surname::shortenedMainWord))
                .map(surname -> {
                    List<EnrichedPerson> persons = searchPersonsBySurname(surname, gedcom);

                    List<String> variants = getSurnameVariants(surname, persons);

                    List<String> countries = persons
                            .stream()
                            .flatMap(person -> Stream.concat(
                                    Stream
                                            .of(
                                                    person.getCountryOfBirth(),
                                                    person.getCountryOfDeath())
                                            .flatMap(Optional::stream),
                                    person
                                            .getCountriesOfMarriage()
                                            .stream()))
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
                                            .getDatesOfMarriage()
                                            .stream()))
                            .map(Date::getYear)
                            .collect(Collectors.summarizingInt(Year::getValue));;

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

    private List<String> getSurnameVariants(
            Surname surname,
            List<EnrichedPerson> persons) {
        return persons
                .stream()
                .map(EnrichedPerson::getSurname)
                .flatMap(Optional::stream)
                .distinct()
                .filter(surname::matches)
                // Skip same surname
                .filter(s -> !StringUtils.startsWithIgnoreCase(s.value(), surname.value()))
                // Try to suppress compound surnames
                .map(this::suppressCompoundSurname)
                .distinct()
                .sorted()
                .toList();
    }

    private String suppressCompoundSurname(Surname surname) {
        if (surname.value().length() > surname.normalizedMainWord().length() + 4) {
            return StringUtils.substringBeforeLast(surname.value(), " ");
        } else {
            return surname.value();
        }
    }

    private List<EnrichedPerson> searchPersonsBySurname(
            Surname surname,
            EnrichedGedcom gedcom) {

        return Arrays.stream(SexType.values())
                .map(sex -> gedcom.getPersonsBySurnameMainWordAndSex(surname, sex))
                .flatMap(List::stream)
                .toList();
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private boolean isValidMinMax(Integer integer) {
        return integer != Integer.MIN_VALUE && integer != Integer.MAX_VALUE;
    }

}
