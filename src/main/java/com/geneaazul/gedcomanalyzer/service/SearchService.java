package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.Age;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResult;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;
import com.geneaazul.gedcomanalyzer.utils.SearchUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {

    public static final int NOT_A_DUPLICATE_SCORE = 100;

    private final GedcomAnalyzerProperties properties;

    public List<EnrichedPerson> findPersonsBySurnameAndSpouseSurname(String personSurname, String spouseSurname, boolean exactMatch, List<EnrichedPerson> people) {
        String personSurnameStr = SearchUtils.simplifyName(personSurname);
        String spouseSurnameStr = spouseSurname != null ? SearchUtils.simplifyName(spouseSurname) : null;

        return people
                .stream()
                .filter(person -> person.getSurnameForSearch()
                        .map(surname -> exactMatch ? surname.equals(personSurnameStr) : surname.contains(personSurnameStr))
                        .orElse(false))
                .filter(person -> spouseSurnameStr == null || person.getSpouses()
                        .stream()
                        .map(EnrichedPerson::getSurnameForSearch)
                        .flatMap(Optional::stream)
                        .anyMatch(surname -> exactMatch ? surname.equals(spouseSurnameStr) : surname.contains(spouseSurnameStr)))
                .toList();
    }

    /**
     * .
     */
    public List<EnrichedPerson> findPersonsByPlaceOfBirth(String placeOfBirth, Boolean isAlive, SexType sex, List<EnrichedPerson> people) {
        return people
                .stream()
                .filter(person -> isAlive == null || isAlive.equals(person.isAlive()))
                .filter(person -> sex == null || sex == person.getSex())
                .filter(person -> person.getPlaceOfBirthForSearch()
                        .map(pob -> pob.endsWith(placeOfBirth))
                        .orElse(false))
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
                        // Compare siblings
                        || person.getSiblings()
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

    public List<PersonComparisonResults> findDuplicatedPersons(EnrichedGedcom gedcom) {
        Set<String> comparedIds = new HashSet<>();

        List<PersonComparisonResults> results = gedcom.getPeople()
                .stream()
                .filter(person -> person.getSurnameMainWordForSearch().isPresent())
                .filter(person -> person.getSex() != SexType.U)
                .map(person -> {
                    comparedIds.add(person.getId());

                    List<PersonComparisonResult> comparisonResults = gedcom
                            .getPersonsBySurnameMainWordAndSex(
                                    person.getSurnameMainWordForSearch().get(),
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
                .sorted(Comparator.comparingInt(PersonComparisonResults::getMinScore))
                .toList();
    }

    private int getDuplicateScore(EnrichedPerson person, EnrichedPerson compare) {

        if (!person.equalsSex(compare)) {
            return NOT_A_DUPLICATE_SCORE;
        }
        if (!person.matchesSurname(compare)) {
            return NOT_A_DUPLICATE_SCORE;
        }
        if (!person.matchesGiven(compare)) {
            return NOT_A_DUPLICATE_SCORE;
        }

        boolean bothHaveParents = !person.getParents().isEmpty() && !compare.getParents().isEmpty();
        boolean pMatchesParents = person.matchesParents(compare);
        boolean cMatchesParents = compare.matchesParents(person);
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
                boolean pMatchesSpouses = person.matchesSpouses(compare);
                boolean cMatchesSpouses = compare.matchesSpouses(person);
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
            Function<String, List<EnrichedPerson>> personsSupplier) {
        return findPersonsByNameAndAnyRelative(
                givenNameAndSurnameSupplier,
                personsSupplier,
                List::of,
                ep -> List.of());
    }

    private List<EnrichedPerson> findPersonsByNameAndAnyRelative(
            Supplier<GivenNameAndSurname> givenNameAndSurnameSupplier,
            Function<String, List<EnrichedPerson>> personsSupplier,
            Supplier<List<GivenNameAndSurname>> relativesGivenNameAndSurnamesSupplier,
            Function<EnrichedPerson, List<EnrichedPerson>> relativesSupplier) {

        GivenNameAndSurname givenNameAndSurname = givenNameAndSurnameSupplier.get();
        if (givenNameAndSurname.isAnyValueEmpty()) {
            return List.of();
        }

        List<EnrichedPerson> persons = personsSupplier.apply(givenNameAndSurname.surname());
        if (persons.isEmpty()) {
            return persons;
        }

        List<GivenNameAndSurname> relativesGivenNameAndSurnames = relativesGivenNameAndSurnamesSupplier.get();
        if (!relativesGivenNameAndSurnames.isEmpty()
                && relativesGivenNameAndSurnames
                        .stream()
                        .allMatch(GivenNameAndSurname::isAnyValueEmpty)) {
            return List.of();
        }

        return persons
                .stream()
                .filter(person -> SearchUtils.matchesGivenName(person.getGivenNameForSearch().orElse(null), givenNameAndSurname.givenName()))
                .filter(person -> relativesGivenNameAndSurnames.isEmpty()
                        || relativesSupplier
                                .apply(person)
                                .stream()
                                .anyMatch(relative -> relativesGivenNameAndSurnames
                                        .stream()
                                        .anyMatch(gns
                                                -> relative.getSurnameMainWordForSearch()
                                                        .map(s -> s.equals(gns.surname()))
                                                        .orElse(false)
                                                && SearchUtils.matchesGivenName(
                                                        relative.getGivenNameForSearch()
                                                                .orElse(null),
                                                        gns.givenName()))))
                .toList();
    }

    public List<EnrichedPerson> findPersonsByName(
            @Nullable String givenName,
            @Nullable String surname,
            @Nullable SexType sex,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(givenName, surname, sex)) {
            return List.of();
        }
        return findPersonsByName(
                () -> GivenNameAndSurname.of(givenName, surname, sex, properties),
                surnameMainWord -> gedcom.getPersonsBySurnameMainWordAndSex(surnameMainWord, sex));
    }

    public List<EnrichedPerson> findPersonsByNameAndYearOfBirth(
            @Nullable String givenName,
            @Nullable String surname,
            @Nullable SexType sex,
            @Nullable Integer yearOfBirth,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(givenName, surname, sex, yearOfBirth)) {
            return List.of();
        }
        return findPersonsByName(
                () -> GivenNameAndSurname.of(givenName, surname, sex, properties),
                surnameMainWord -> gedcom.getPersonsBySurnameMainWordAndSexAndYearOfBirthIndex(surnameMainWord, sex, Year.of(yearOfBirth)));
    }

    public List<EnrichedPerson> findPersonsByNameAndYearOfDeath(
            @Nullable String givenName,
            @Nullable String surname,
            @Nullable SexType sex,
            @Nullable Integer yearOfDeath,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(givenName, surname, sex, yearOfDeath)) {
            return List.of();
        }
        return findPersonsByName(
                () -> GivenNameAndSurname.of(givenName, surname, sex, properties),
                surnameMainWord -> gedcom.getPersonsBySurnameMainWordAndSexAndYearOfDeathIndex(surnameMainWord, sex, Year.of(yearOfDeath)));
    }

    public List<EnrichedPerson> findPersonsByNameAndParentsNames(
            @Nullable String givenName, @Nullable String surname, @Nullable SexType sex,
            @Nullable String parent1GivenName, @Nullable String parent1Surname, @Nullable SexType parent1Sex,
            @Nullable String parent2GivenName, @Nullable String parent2Surname, @Nullable SexType parent2Sex,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(givenName, surname, sex)
                || ObjectUtils.anyNull(parent1GivenName, parent1Surname, parent1Sex)
                && ObjectUtils.anyNull(parent2GivenName, parent2Surname, parent2Sex)) {
            return List.of();
        }
        return findPersonsByNameAndAnyRelative(
                () -> GivenNameAndSurname.of(givenName, surname, sex, properties),
                surnameMainWord -> gedcom.getPersonsBySurnameMainWordAndSex(surnameMainWord, sex),
                () -> List.of(
                        GivenNameAndSurname.of(parent1GivenName, parent1Surname, parent1Sex, properties),
                        GivenNameAndSurname.of(parent2GivenName, parent2Surname, parent2Sex, properties)),
                EnrichedPerson::getParents);
    }

    public List<EnrichedPerson> findPersonsByNameAndSpouseName(
            @Nullable String givenName, @Nullable String surname, @Nullable SexType sex,
            @Nullable String spouseGivenName, @Nullable String spouseSurname, @Nullable SexType spouseSex,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(givenName, surname, sex)
                || ObjectUtils.anyNull(spouseGivenName, spouseSurname, spouseSex)) {
            return List.of();
        }
        return findPersonsByNameAndAnyRelative(
                () -> GivenNameAndSurname.of(givenName, surname, sex, properties),
                surnameMainWord -> gedcom.getPersonsBySurnameMainWordAndSex(surnameMainWord, sex),
                () -> List.of(
                        GivenNameAndSurname.of(spouseGivenName, spouseSurname, spouseSex, properties)),
                EnrichedPerson::getSpouses);
    }

    public List<EnrichedPerson> findPersonsBySurname(
            @Nullable String surname,
            @Nullable SexType sex,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(surname, sex)) {
            return List.of();
        }
        return PersonUtils.getSurnameMainWordForSearch(surname, properties.getNormalizedSurnamesMap())
                .map(surnameMainWord -> gedcom.getPersonsBySurnameMainWordAndSex(surnameMainWord, sex))
                .orElseGet(List::of);
    }

    public List<EnrichedPerson> findPersonsBySurnameAndYearOfBirth(
            @Nullable String surname,
            @Nullable SexType sex,
            @Nullable Integer yearOfBirth,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(surname, sex, yearOfBirth)) {
            return List.of();
        }
        return PersonUtils.getSurnameMainWordForSearch(surname, properties.getNormalizedSurnamesMap())
                .map(surnameMainWord -> gedcom.getPersonsBySurnameMainWordAndSexAndYearOfBirthIndex(surnameMainWord, sex, Year.of(yearOfBirth)))
                .orElseGet(List::of);
    }

    public List<EnrichedPerson> findPersonsBySurnameAndYearOfDeath(
            @Nullable String surname,
            @Nullable SexType sex,
            @Nullable Integer yearOfDeath,
            EnrichedGedcom gedcom) {
        if (ObjectUtils.anyNull(surname, sex, yearOfDeath)) {
            return List.of();
        }
        return PersonUtils.getSurnameMainWordForSearch(surname, properties.getNormalizedSurnamesMap())
                .map(surnameMainWord -> gedcom.getPersonsBySurnameMainWordAndSexAndYearOfDeathIndex(surnameMainWord, sex, Year.of(yearOfDeath)))
                .orElseGet(List::of);
    }

    private record GivenNameAndSurname(GivenName givenName, String surname) {

        public boolean isAnyValueEmpty() {
            return givenName == null || surname == null;
        }

        public static GivenNameAndSurname of(
                @Nullable String givenName,
                @Nullable String surname,
                @Nullable SexType sex,
                GedcomAnalyzerProperties properties) {
            Optional<GivenName> givenNameForSearch = PersonUtils.getNormalizedGivenNameForSearch(givenName, sex, properties.getNormalizedNamesMap());
            Optional<String> surnameMainWordForSearch = PersonUtils.getSurnameMainWordForSearch(surname, properties.getNormalizedSurnamesMap());
            return new GivenNameAndSurname(
                    givenNameForSearch.orElse(null),
                    surnameMainWordForSearch.orElse(null));
        }
    }

}
