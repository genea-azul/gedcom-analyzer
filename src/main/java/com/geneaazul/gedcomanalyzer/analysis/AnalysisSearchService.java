package com.geneaazul.gedcomanalyzer.analysis;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.springframework.stereotype.Service;

import org.apache.commons.lang3.StringUtils;

import java.time.Month;
import java.time.Year;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Person searches used only by the offline analysis tools (data-quality checks, date and place
 * lookups); the API's searches live in {@link com.geneaazul.gedcomanalyzer.service.SearchService}.
 */
@Service
@RequiredArgsConstructor
public class AnalysisSearchService {

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsByMonthAndDayOfBirth(
            Month month,
            int day,
            @Nullable SexType sex,
            List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person -> person.getDateOfBirth()
                        .filter(Date::isFullDate)
                        .map(dob -> dob.getMonth().equals(month) && dob.getDay().equals(day))
                        .orElse(false))
                .toList();
    }

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsByMonthAndDayOfDeath(
            Month month,
            int day,
            @Nullable SexType sex,
            List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> !person.isAlive())
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person -> person.getDateOfDeath()
                        .filter(Date::isFullDate)
                        .map(dob -> dob.getMonth().equals(month) && dob.getDay().equals(day))
                        .orElse(false))
                .toList();
    }

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsByYearOfDeathAndNoParents(
            Year year,
            @Nullable SexType sex,
            List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> !person.isAlive())
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person -> person.getDateOfDeath()
                        .map(dob -> dob.getYear().equals(year))
                        .orElse(false))
                .filter(person -> person.getParents().isEmpty())
                .toList();
    }

    /**
     *
     */
    public List<EnrichedPerson> findPersonsByDateOfBirthBetween(
            Date dateFrom,
            Date dateTo,
            List<EnrichedPerson> people) {
        Date dateToExclusive = Date.from(dateTo.toLocalDate().plusDays(1));
        return people
                .stream()
                .filter(person -> person.getDateOfBirth()
                        .filter(Date::isFullDate)
                        .map(dob -> !dob.isBefore(dateFrom) && dob.isBefore(dateToExclusive))
                        .orElse(false))
                .sorted(Comparator.comparing(p -> p.getDateOfBirth().get()))
                .toList();
    }

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsByPlaceOfBirth(
            @NonNull String placeOfBirth,
            @Nullable Boolean isAlive,
            @Nullable SexType sex,
            boolean isExactPlace,
            List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> isAlive == null || isAlive == person.isAlive())
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person -> person
                        .getPlaceOfBirth()
                        .map(Place::forSearch)
                        .map(pob -> isExactPlace
                                ? pob.equals(placeOfBirth)
                                : pob.endsWith(placeOfBirth))
                        .orElse(false))
                .toList();
    }

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsByPlaceOfDeath(
            @NonNull String placeOfDeath,
            @Nullable SexType sex,
            boolean isExactPlace,
            List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> !person.isAlive())
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person -> person
                        .getPlaceOfDeath()
                        .map(Place::forSearch)
                        .map(pod -> isExactPlace
                                ? pod.equals(placeOfDeath)
                                : pod.endsWith(placeOfDeath))
                        .orElse(false))
                .toList();
    }

    public List<EnrichedPerson> findPersonsWithCustomEventFacts(List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> !person.getCustomEventFacts().isEmpty() || !person.getFamilyCustomEventFacts().isEmpty())
                .toList();
    }

    public List<EnrichedPerson> findPersonsWithTagExtensions(List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> !person.getTagExtensions().isEmpty())
                .toList();
    }

    public List<EnrichedPerson> findPersonsWithNoCountryButParentsWithCountry(List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> person.getPlaceOfBirth().isEmpty()
                        && !person.getParents().isEmpty()
                        && person.getParents()
                                .stream()
                                .allMatch(parent -> parent.getPlaceOfBirth().isPresent()))
                .toList();
    }

    public List<Surname> findSurnamesByPattern(String regex, List<EnrichedPerson> people) {
        Pattern pattern = Pattern.compile(regex);
        Comparator<Surname> comparator = Comparator.comparing(Surname::value);
        return people
                .stream()
                .map(EnrichedPerson::getSurname)
                .flatMap(Optional::stream)
                .filter(surname -> pattern.matcher(surname.value()).find())
                .distinct()
                .sorted(comparator)
                .toList();
    }

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsWithMisspellingByPlaceOfBirth(
            @Nullable String countryOfBirth,
            @Nullable Boolean isAlive,
            @Nullable SexType sex,
            List<EnrichedPerson> people) {

        List<EnrichedPerson> filteredPeople = people
                .stream()
                .filter(person -> isAlive == null || isAlive == person.isAlive())
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person
                        -> countryOfBirth == null && person.getPlaceOfBirth().isEmpty()
                        || countryOfBirth != null && person
                                .getPlaceOfBirth()
                                .map(Place::country)
                                .map(countryOfBirth::equals)
                                .orElse(false))
                .toList();

        List<String> givenNames = filteredPeople
                .stream()
                .map(EnrichedPerson::getGivenName)
                .flatMap(Optional::stream)
                .map(GivenName::value)
                .flatMap(givenName -> Arrays.stream(givenName.split(" ")))
                .map(givenName -> StringUtils.stripStart(givenName, "("))
                .map(givenName -> StringUtils.stripEnd(givenName, ")"))
                .distinct()
                .toList();

        Set<String> misspelledGivenNames;

        // Spanish-speaking countries
        if (Set.of("Argentina", "Chile", "España", "Paraguay", "Perú", "Uruguay").contains(countryOfBirth)) {

            List<String> expectedSpelledWithAccentsGivenNames = givenNames
                    .stream()
                    .filter(givenName
                            -> !Set.of("Cristián", "Álida", "Rosalía", "Yésica").contains(givenName)
                            && Stream.of("Á", "É", "Í", "Ó", "Ú", "Ü", "á", "é", "í", "ó", "ú", "ü").anyMatch(givenName::contains))
                    .distinct()
                    .sorted()
                    .toList();

            List<String> expectedMisspelledGivenNames = givenNames
                    .stream()
                    .filter(givenName
                            -> Stream.of("À", "È", "Ì", "Ò", "Ù", "à", "è", "ì", "ò", "ù").anyMatch(givenName::contains)
                            || !Set.of("de", "del", "la", "las", "los", "o").contains(givenName) && Character.isLowerCase(givenName.charAt(0)))
                    .distinct()
                    .sorted()
                    .toList();

            misspelledGivenNames = Stream
                    .concat(
                            expectedSpelledWithAccentsGivenNames
                                    .stream()
                                    .map(StringUtils::stripAccents),
                            expectedMisspelledGivenNames
                                    .stream())
                    .collect(Collectors.toUnmodifiableSet());

        // Italian-speaking countries
        } else if (Set.of("Austria", "Italia", "Suiza").contains(countryOfBirth)) {

            //noinspection RedundantCollectionOperation
            misspelledGivenNames = givenNames
                    .stream()
                    .filter(givenName
                            -> Set.of("Vicenzo", "Vicenza").contains(givenName)
                            || !Set.of("del").contains(givenName) && Character.isLowerCase(givenName.charAt(0)))
                    .collect(Collectors.toUnmodifiableSet());

        } else {
            misspelledGivenNames = Set.of();
        }

        return filteredPeople
                .stream()
                .filter(person -> person.getGivenName()
                        .map(GivenName::value)
                        .stream()
                        .flatMap(givenName -> Arrays.stream(givenName.split(" ")))
                        .map(givenName -> StringUtils.stripStart(givenName, "("))
                        .map(givenName -> StringUtils.stripEnd(givenName, ")"))
                        .anyMatch(misspelledGivenNames::contains))
                .toList();
    }

}
