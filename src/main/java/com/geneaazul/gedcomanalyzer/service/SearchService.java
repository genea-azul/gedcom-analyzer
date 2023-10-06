package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.Age;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.GivenNameAndSurname;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResult;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;
import com.geneaazul.gedcomanalyzer.utils.NameUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {

    public static final int NOT_A_DUPLICATE_SCORE = 100;

    private final GedcomAnalyzerProperties properties;

    public List<EnrichedPerson> findPersonsByNameAndSpouseName(
            @Nullable String personGivenName,
            @Nullable String personSurname,
            @Nullable String spouseGivenName,
            @Nullable String spouseSurname,
            boolean exactMatch,
            List<EnrichedPerson> people) {
        String personGivenNameStr = NameUtils.simplifyName(personGivenName);
        String personSurnameStr = NameUtils.simplifyName(personSurname);
        String spouseGivenNameStr = NameUtils.simplifyName(spouseGivenName);
        String spouseSurnameStr = NameUtils.simplifyName(spouseSurname);

        if (personGivenNameStr == null && personSurnameStr == null
                || spouseGivenNameStr == null && spouseSurnameStr == null) {
            return List.of();
        }

        return people
                .stream()
                .filter(person
                        -> personGivenNameStr == null
                        || this.evalPersonName(person, EnrichedPerson::getGivenName, name -> NameUtils.simplifyName(name.value()), personGivenNameStr, exactMatch))
                .filter(person
                        -> personSurnameStr == null
                        || this.evalPersonName(person, EnrichedPerson::getSurname, name -> NameUtils.simplifyName(name.value()), personSurnameStr, exactMatch)
                        || this.evalSpouseName(person, EnrichedPerson::getAka, NameUtils::simplifyName, personSurnameStr, exactMatch))
                .filter(person
                        -> spouseGivenNameStr == null
                        || this.evalSpouseName(person, EnrichedPerson::getGivenName, name -> NameUtils.simplifyName(name.value()), spouseGivenNameStr, exactMatch))
                .filter(person
                        -> spouseSurnameStr == null
                        || this.evalSpouseName(person, EnrichedPerson::getSurname, name -> NameUtils.simplifyName(name.value()), spouseSurnameStr, exactMatch)
                        || this.evalSpouseName(person, EnrichedPerson::getAka, NameUtils::simplifyName, spouseSurnameStr, exactMatch))
                .toList();
    }

    private <T> boolean evalPersonName(
            EnrichedPerson person,
            Function<EnrichedPerson, Optional<T>> map1,
            Function<T, String> map2,
            String personName,
            boolean exactMatch) {
        return map1.apply(person)
                .map(map2)
                .map(name -> exactMatch ? name.equals(personName) : name.contains(personName))
                .orElse(false);
    }

    private <T> boolean evalSpouseName(
            EnrichedPerson person,
            Function<EnrichedPerson, Optional<T>> map1,
            Function<T, String> map2,
            String spouseName,
            boolean exactMatch) {
        return person.getSpouses()
                .stream()
                .map(map1)
                .flatMap(Optional::stream)
                .map(map2)
                .anyMatch(name -> exactMatch ? name.equals(spouseName) : name.contains(spouseName));
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
                        .map(dob -> dob.getMonth() == month && dob.getDay() == day)
                        .orElse(false))
                .toList();
    }

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsByPlaceOfBirth(
            @NonNull String placeOfBirth,
            @Nullable Boolean isAlive,
            @Nullable SexType sex,
            List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> isAlive == null || isAlive == person.isAlive())
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person -> person
                        .getPlaceOfBirth()
                        .map(Place::forSearch)
                        .map(pob -> pob.endsWith(placeOfBirth))
                        .orElse(false))
                .toList();
    }

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsByPlaceOfDeath(
            @NonNull String placeOfDeath,
            @Nullable SexType sex,
            List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> !person.isAlive())
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person -> person
                        .getPlaceOfDeath()
                        .map(Place::forSearch)
                        .map(pod -> pod.endsWith(placeOfDeath))
                        .orElse(false))
                .toList();
    }

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsByPlaceOfAnyEvent(
            @NonNull String placeOfEvent,
            @Nullable Boolean isAlive,
            @Nullable SexType sex,
            List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> isAlive == null || isAlive == person.isAlive())
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person -> person
                        .getPlacesOfAnyEvent()
                        .stream()
                        .map(Place::forSearch)
                        .anyMatch(place -> place.endsWith(placeOfEvent)))
                .toList();
    }

    public List<EnrichedPerson> findAlivePersonsTooOldOrWithFamilyMembersTooOld(List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(EnrichedPerson::isAlive)
                .filter(person -> isDateOfBirthBefore(person, properties.getPersonMinDateOfBirth())
                        // Compare parents
                        || person.getParents()
                                .stream()
                                .anyMatch(parent
                                        -> isDateOfBirthBefore(parent, properties.getParentMinDateOfBirth())
                                        || isDateOfDeathBefore(parent, properties.getParentMinDateOfDeath()))
                        // Compare all siblings (full and half)
                        || person.getAllSiblings()
                                .stream()
                                .anyMatch(sibling
                                        -> isDateOfBirthBefore(sibling, properties.getSiblingMinDateOfBirth())
                                        || isDateOfDeathBefore(sibling, properties.getSiblingMinDateOfDeath()))
                        // Compare spouses
                        || person.getSpouses()
                                .stream()
                                .anyMatch(spouse
                                        -> isDateOfBirthBefore(spouse, properties.getSpouseMinDateOfBirth())
                                        || isDateOfDeathBefore(spouse, properties.getSpouseMinDateOfDeath()))
                        // Compare children
                        || person.getChildren()
                                .stream()
                                .anyMatch(child
                                        -> isDateOfBirthBefore(child, properties.getChildMinDateOfBirth())
                                        || isDateOfDeathBefore(child, properties.getChildMinDateOfDeath())))
                .toList();
    }

    private boolean isDateOfBirthBefore(EnrichedPerson person, LocalDate minDateOfBirth) {
        return person.getDateOfBirth()
                .map(dob -> dob.isBefore(minDateOfBirth))
                .orElse(false);
    }

    private boolean isDateOfDeathBefore(EnrichedPerson person, LocalDate minDateOfDeath) {
        return person.getDateOfDeath()
                .map(dod -> dod.isBefore(minDateOfDeath))
                .orElse(false);
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

    public List<PersonComparisonResults> findDuplicatedPersons(EnrichedGedcom gedcom) {
        Set<String> comparedIds = new HashSet<>();

        List<PersonComparisonResults> results = gedcom.getPeople()
                .stream()
                .filter(person -> person.getSurname().isPresent())
                .filter(person -> person.getSex() != SexType.U)
                .map(person -> {
                    comparedIds.add(person.getId());

                    List<PersonComparisonResult> comparisonResults = gedcom
                            .getPersonsBySurnameMainWordAndSex(
                                    person.getSurname().get(),
                                    person.getSex())
                            .stream()
                            .filter(compare -> !compare.getId().equals(person.getId()))
                            .filter(compare -> !comparedIds.contains(compare.getId()))
                            .map(compare -> {
                                int duplicateScore = getDuplicateScore(person, compare);

                                if (duplicateScore < NOT_A_DUPLICATE_SCORE) {
                                    return PersonComparisonResult.of(compare, duplicateScore);
                                } else {
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .toList();

                    if (CollectionUtils.isNotEmpty(comparisonResults)) {
                        return PersonComparisonResults.of(person, comparisonResults);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return results
                .stream()
                .sorted(Comparator.comparingInt(PersonComparisonResults::minScore))
                .toList();
    }

    private int getDuplicateScore(EnrichedPerson person, EnrichedPerson compare) {

        if (!person.equalsSex(compare)
                || !person.matchesGivenNameAndSurname(compare)) {
            return NOT_A_DUPLICATE_SCORE;
        }

        boolean bothHaveParents = !person.getParents().isEmpty() && !compare.getParents().isEmpty();
        boolean pMatchesParents = person.matchesAllParents(compare);
        boolean cMatchesParents = compare.matchesAllParents(person);
        boolean matchesParents = bothHaveParents && (pMatchesParents || cMatchesParents);
        boolean fullMatchesParents = bothHaveParents && pMatchesParents && cMatchesParents;

        boolean notBothHaveDobSet = person.getDateOfBirth().isEmpty() || compare.getDateOfBirth().isEmpty();
        boolean notBothHaveDodSet = person.getDateOfDeath().isEmpty() || compare.getDateOfDeath().isEmpty();
        boolean notBothHaveDobOrDodSet = notBothHaveDobSet && notBothHaveDodSet;

        if (notBothHaveDobOrDodSet) {

            if (matchesParents || !bothHaveParents) {
                // Skip persons not matching dob and dod
                if (isNotValidAge(person.getDateOfBirth(), compare.getDateOfDeath())
                        || isNotValidAge(compare.getDateOfBirth(), person.getDateOfDeath())) {
                    return NOT_A_DUPLICATE_SCORE;
                }
            }

            // Look for same parents
            if (matchesParents) {
                if (person.getParents().size() == 1) {
                    return fullMatchesParents ? 91 : 92;
                }
                return fullMatchesParents ? 30 : 90;

            } else if (!bothHaveParents) {

                // Look for matching spouses
                boolean bothHaveSpouses = !person.getSpouses().isEmpty() && !compare.getSpouses().isEmpty();
                boolean pMatchesSpouses = person.matchesAnySpouses(compare);
                boolean cMatchesSpouses = compare.matchesAnySpouses(person);
                boolean matchesSpouses = bothHaveSpouses && (pMatchesSpouses || cMatchesSpouses);

                if (matchesSpouses) {
                    return 50;
                }
            }

        } else {
            if (person.matchesDateOfBirthByDay(compare)) {
                if (!(bothHaveParents && !matchesParents && !person.equalsDateOfBirthByDay(compare))) {
                    return 10;
                }
            }
            if (person.matchesDateOfDeathByDay(compare)) {
                if (!(bothHaveParents && !matchesParents && !person.equalsDateOfDeathByDay(compare))) {
                    return 11;
                }
            }

            // Dates are not matched by day, we can discard persons with existing but not matching parents
            if (!bothHaveParents || matchesParents) {
                if (person.matchesDateOfBirthByMonth(compare) && (notBothHaveDodSet || person.matchesDateOfDeathByAny(compare))) {
                    return fullMatchesParents ? 60 : 65;
                }
                if (person.matchesDateOfDeathByMonth(compare) && (notBothHaveDobSet || person.matchesDateOfBirthByAny(compare))) {
                    return fullMatchesParents ? 61 : 66;
                }
                if (person.matchesDateOfBirthByYear(compare) && (notBothHaveDodSet || person.matchesDateOfDeathByAny(compare))) {
                    return fullMatchesParents ? 80 : 85;
                }
                if (person.matchesDateOfDeathByYear(compare) && (notBothHaveDobSet || person.matchesDateOfBirthByAny(compare))) {
                    return fullMatchesParents ? 81 : 86;
                }
            }
        }

        return NOT_A_DUPLICATE_SCORE;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean isNotValidAge(Optional<Date> dateOfBirth, Optional<Date> dateOfDeath) {
        Optional<Age> age = Age.of(dateOfBirth, dateOfDeath);
        return age.isPresent() && age.get().getYears() > properties.getAlivePersonMaxAge();
    }

    private List<EnrichedPerson> findPersonsByName(
            Supplier<GivenNameAndSurname> givenNameAndSurnameSupplier,
            Function<Surname, List<EnrichedPerson>> personsSupplier) {
        return findPersonsByNameAndAnyRelative(
                givenNameAndSurnameSupplier,
                personsSupplier,
                Optional::empty,
                ep -> List.of(),
                false);
    }

    private List<EnrichedPerson> findPersonsByNameAndAnyRelative(
            Supplier<GivenNameAndSurname> personGivenNameAndSurnameToCompareSupplier,
            Function<Surname, List<EnrichedPerson>> personsSupplier,
            Supplier<Optional<List<GivenNameAndSurname>>> relativesGivenNameAndSurnamesToCompareSupplier,
            Function<EnrichedPerson, List<EnrichedPerson>> relativesSupplier,
            boolean isAllRelativesGivenNameAndSurnamesToCompareMatch) {

        GivenNameAndSurname personGivenNameAndSurnameToCompare = personGivenNameAndSurnameToCompareSupplier.get();
        if (personGivenNameAndSurnameToCompare.isAnyValueEmpty()) {
            return List.of();
        }

        List<EnrichedPerson> persons = personsSupplier.apply(personGivenNameAndSurnameToCompare.surname());
        if (persons.isEmpty()) {
            return persons;
        }

        Optional<List<GivenNameAndSurname>> relativesGivenNameAndSurnamesToCompare = relativesGivenNameAndSurnamesToCompareSupplier.get();

        if (relativesGivenNameAndSurnamesToCompare.isEmpty()) {
            return persons
                    .stream()
                    .filter(person -> person.matchesGivenNameAndSurname(personGivenNameAndSurnameToCompare))
                    .toList();
        }

        List<GivenNameAndSurname> validRelativesGivenNameAndSurnamesToCompare = relativesGivenNameAndSurnamesToCompare
                .get()
                .stream()
                .filter(GivenNameAndSurname::areAllValuesNotEmpty)
                .toList();

        if (validRelativesGivenNameAndSurnamesToCompare.isEmpty()) {
            return List.of();
        }

        BiPredicate<GivenNameAndSurname, List<EnrichedPerson>> givenNameAndSurnameMatcher = (givenNameAndSurnameSearch, personsToMatch) -> personsToMatch
                .stream()
                .anyMatch(personToMatch -> personToMatch.matchesGivenNameAndSurname(givenNameAndSurnameSearch));

        Predicate<List<EnrichedPerson>> relativesMatcher = isAllRelativesGivenNameAndSurnamesToCompareMatch
                ? relativesToMatch -> validRelativesGivenNameAndSurnamesToCompare
                        .stream()
                        .allMatch(givenNameAndSurnameSearch -> givenNameAndSurnameMatcher.test(givenNameAndSurnameSearch, relativesToMatch))
                : relativesToMatch -> validRelativesGivenNameAndSurnamesToCompare
                        .stream()
                        .anyMatch(givenNameAndSurnameSearch -> givenNameAndSurnameMatcher.test(givenNameAndSurnameSearch, relativesToMatch));

        return persons
                .stream()
                .filter(person -> person.matchesGivenNameAndSurname(personGivenNameAndSurnameToCompare))
                .filter(person -> relativesMatcher.test(relativesSupplier.apply(person)))
                .toList();
    }

    public List<EnrichedPerson> findPersonsByName(
            @Nullable String givenName,
            @Nullable String surname,
            @Nullable SexType sex,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(givenName, surname)) {
            return List.of();
        }

        List<SexType> personSexes = PersonUtils.getOrValidValues(sex);

        return personSexes
                .stream()
                .map(pSex -> findPersonsByName(
                        () -> GivenNameAndSurname.of(givenName, surname, pSex, properties),
                        s -> gedcom.getPersonsBySurnameMainWordAndSex(s, pSex)))
                .flatMap(List::stream)
                .toList();
    }

    @SuppressWarnings("DataFlowIssue")
    public List<EnrichedPerson> findPersonsByNameAndYearOfBirth(
            @Nullable String givenName,
            @Nullable String surname,
            @Nullable SexType sex,
            @Nullable Integer yearOfBirth,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(givenName, surname, yearOfBirth)) {
            return List.of();
        }

        List<SexType> personSexes = PersonUtils.getOrValidValues(sex);

        return personSexes
                .stream()
                .map(pSex -> findPersonsByName(
                        () -> GivenNameAndSurname.of(givenName, surname, pSex, properties),
                        s -> gedcom.getPersonsBySurnameMainWordAndSexAndYearOfBirthIndex(s, pSex, Year.of(yearOfBirth))))
                .flatMap(List::stream)
                .toList();
    }

    @SuppressWarnings("DataFlowIssue")
    public List<EnrichedPerson> findPersonsByNameAndYearOfDeath(
            @Nullable String givenName,
            @Nullable String surname,
            @Nullable SexType sex,
            @Nullable Integer yearOfDeath,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(givenName, surname, yearOfDeath)) {
            return List.of();
        }

        List<SexType> personSexes = PersonUtils.getOrValidValues(sex);

        return personSexes
                .stream()
                .map(pSex -> findPersonsByName(
                        () -> GivenNameAndSurname.of(givenName, surname, pSex, properties),
                        s -> gedcom.getPersonsBySurnameMainWordAndSexAndYearOfDeathIndex(s, pSex, Year.of(yearOfDeath))))
                .flatMap(List::stream)
                .toList();
    }

    public List<EnrichedPerson> findPersonsByNameAndParentsNames(
            @Nullable String personGivenName, @Nullable String personSurname, @Nullable SexType personSex,
            @Nullable String parent1GivenName, @Nullable String parent1Surname, @Nullable SexType parent1Sex,
            @Nullable String parent2GivenName, @Nullable String parent2Surname, @Nullable SexType parent2Sex,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(personGivenName, personSurname)
                || ObjectUtils.anyNull(parent1GivenName, parent1Surname, parent1Sex)
                && ObjectUtils.anyNull(parent2GivenName, parent2Surname, parent2Sex)) {
            return List.of();
        }

        List<SexType> personSexes = PersonUtils.getOrValidValues(personSex);

        return personSexes
                .stream()
                .map(pSex -> findPersonsByNameAndAnyRelative(
                        () -> GivenNameAndSurname.of(personGivenName, personSurname, pSex, properties),
                        surname-> gedcom.getPersonsBySurnameMainWordAndSex(surname, pSex),
                        () -> Optional.of(List.of(
                                GivenNameAndSurname.of(parent1GivenName, parent1Surname, parent1Sex, properties),
                                GivenNameAndSurname.of(parent2GivenName, parent2Surname, parent2Sex, properties))),
                        EnrichedPerson::getParents,
                        true))
                .flatMap(List::stream)
                .toList();
    }

    public List<EnrichedPerson> findPersonsByNameAndSpouseName(
            @Nullable String personGivenName, @Nullable String personSurname, @Nullable SexType personSex,
            @Nullable String spouseGivenName, @Nullable String spouseSurname, @Nullable SexType spouseSex,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(personGivenName, personSurname)
                || ObjectUtils.anyNull(spouseGivenName, spouseSurname)) {
            return List.of();
        }

        List<SexType> personSexes = PersonUtils.getOrValidValues(personSex);
        List<SexType> spouseSexes = PersonUtils.getOrValidValues(spouseSex);

        return personSexes
                .stream()
                .map(pSex -> findPersonsByNameAndAnyRelative(
                        () -> GivenNameAndSurname.of(personGivenName, personSurname, pSex, properties),
                        surname -> gedcom.getPersonsBySurnameMainWordAndSex(surname, pSex),
                        () -> Optional.of(spouseSexes
                                .stream()
                                .map(sSex -> GivenNameAndSurname.of(spouseGivenName, spouseSurname, sSex, properties))
                                .toList()),
                        EnrichedPerson::getSpouses,
                        false))
                .flatMap(List::stream)
                .toList();
    }

    public List<EnrichedPerson> findPersonsBySurname(
            @Nullable String surname,
            @Nullable SexType sex,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(surname)) {
            return List.of();
        }

        List<SexType> personSexes = PersonUtils.getOrValidValues(sex);

        return PersonUtils.getShortenedSurnameMainWord(surname, properties.getNormalizedSurnamesMap())
                .map(shortenedSurname -> personSexes
                        .stream()
                        .map(pSex -> gedcom.getPersonsBySurnameMainWordAndSex(shortenedSurname, pSex))
                        .flatMap(List::stream)
                        .toList())
                .orElseGet(List::of);
    }

    @SuppressWarnings("DataFlowIssue")
    public List<EnrichedPerson> findPersonsBySurnameAndYearOfBirth(
            @Nullable String surname,
            @Nullable SexType sex,
            @Nullable Integer yearOfBirth,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(surname, yearOfBirth)) {
            return List.of();
        }

        List<SexType> personSexes = PersonUtils.getOrValidValues(sex);

        return PersonUtils.getShortenedSurnameMainWord(surname, properties.getNormalizedSurnamesMap())
                .map(shortenedSurname -> personSexes
                        .stream()
                        .map(pSex -> gedcom.getPersonsBySurnameMainWordAndSexAndYearOfBirthIndex(shortenedSurname, pSex, Year.of(yearOfBirth)))
                        .flatMap(List::stream)
                        .toList())
                .orElseGet(List::of);
    }

    @SuppressWarnings("DataFlowIssue")
    public List<EnrichedPerson> findPersonsBySurnameAndYearOfDeath(
            @Nullable String surname,
            @Nullable SexType sex,
            @Nullable Integer yearOfDeath,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(surname, yearOfDeath)) {
            return List.of();
        }

        List<SexType> personSexes = PersonUtils.getOrValidValues(sex);

        return PersonUtils.getShortenedSurnameMainWord(surname, properties.getNormalizedSurnamesMap())
                .map(shortenedSurname -> personSexes
                        .stream()
                        .map(pSex -> gedcom.getPersonsBySurnameMainWordAndSexAndYearOfDeathIndex(shortenedSurname, pSex, Year.of(yearOfDeath)))
                        .flatMap(List::stream)
                        .toList())
                .orElseGet(List::of);
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
                            && (StringUtils.containsAny(givenName, "Á", "É", "Í", "Ó", "Ú", "Ü", "á", "é", "í", "ó", "ú", "ü")))
                    .distinct()
                    .sorted()
                    .toList();

            List<String> expectedMisspelledGivenNames = givenNames
                    .stream()
                    .filter(givenName
                            -> StringUtils.containsAny(givenName, "À", "È", "Ì", "Ò", "Ù", "à", "è", "ì", "ò", "ù")
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
