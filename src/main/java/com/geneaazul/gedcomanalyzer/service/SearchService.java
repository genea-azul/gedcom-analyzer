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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    public List<EnrichedPerson> findPersonsByNameAndYearOfBirth(
            String givenName, String surname, SexType sex, Integer yearOfBirth, EnrichedGedcom gedcom) {

        if (sex == null || yearOfBirth == null) {
            return List.of();
        }

        Optional<GivenName> givenNameForSearch = PersonUtils.getNormalizedGivenNameForSearch(givenName, sex, properties.getNormalizedNamesMap());
        Optional<String> surnameMainWordForSearch = PersonUtils.getSurnameMainWordForSearch(surname, properties.getNormalizedSurnamesMap());

        if (givenNameForSearch.isEmpty() || surnameMainWordForSearch.isEmpty()) {
            return List.of();
        }

        return gedcom
                .getPersonsBySurnameMainWordAndSexAndYearOfBirthIndex(surnameMainWordForSearch.get(), sex, Year.of(yearOfBirth))
                .stream()
                .filter(person -> person.getGivenNameForSearch().isPresent())
                .filter(person -> SearchUtils.matchesGivenName(person.getGivenNameForSearch().get(), givenNameForSearch.get()))
                .toList();
    }

    public List<EnrichedPerson> findPersonsByNameAndYearOfDeath(
            String givenName, String surname, SexType sex, Integer yearOfDeath, EnrichedGedcom gedcom) {

        if (sex == null || yearOfDeath == null) {
            return List.of();
        }

        Optional<GivenName> givenNameForSearch = PersonUtils.getNormalizedGivenNameForSearch(givenName, sex, properties.getNormalizedNamesMap());
        Optional<String> surnameMainWordForSearch = PersonUtils.getSurnameMainWordForSearch(surname, properties.getNormalizedSurnamesMap());

        if (givenNameForSearch.isEmpty() || surnameMainWordForSearch.isEmpty()) {
            return List.of();
        }

        return gedcom
                .getPersonsBySurnameMainWordAndSexAndYearOfDeathIndex(surnameMainWordForSearch.get(), sex, Year.of(yearOfDeath))
                .stream()
                .filter(person -> person.getGivenNameForSearch().isPresent())
                .filter(person -> SearchUtils.matchesGivenName(person.getGivenNameForSearch().get(), givenNameForSearch.get()))
                .toList();
    }

    public List<EnrichedPerson> findPersonsByNameAndParentsNames(
            String givenName, String surname, SexType sex,
            String fatherGivenName, String fatherSurname,
            String motherGivenName, String motherSurname,
            EnrichedGedcom gedcom) {

        if (sex == null) {
            return List.of();
        }

        GivenName givenNameForSearch = PersonUtils.getNormalizedGivenNameForSearch(givenName, sex, properties.getNormalizedNamesMap())
                .orElse(null);
        String surnameMainWordForSearch = PersonUtils.getSurnameMainWordForSearch(surname, properties.getNormalizedSurnamesMap())
                .orElse(null);

        if (givenNameForSearch == null || surnameMainWordForSearch == null) {
            return List.of();
        }

        GivenName fatherGivenNameForSearch = PersonUtils.getNormalizedGivenNameForSearch(fatherGivenName, sex, properties.getNormalizedNamesMap())
                .orElse(null);
        String fatherSurnameMainWordForSearch = PersonUtils.getSurnameMainWordForSearch(fatherSurname, properties.getNormalizedSurnamesMap())
                .orElse(null);
        GivenName motherGivenNameForSearch = PersonUtils.getNormalizedGivenNameForSearch(motherGivenName, sex, properties.getNormalizedNamesMap())
                .orElse(null);
        String motherSurnameMainWordForSearch = PersonUtils.getSurnameMainWordForSearch(motherSurname, properties.getNormalizedSurnamesMap())
                .orElse(null);

        if ((fatherGivenNameForSearch == null || fatherSurnameMainWordForSearch == null)
                && (motherGivenNameForSearch == null || motherSurnameMainWordForSearch == null)) {
            return List.of();
        }

        return gedcom
                .getPersonsBySurnameMainWordAndSex(surnameMainWordForSearch, sex)
                .stream()
                .filter(person -> person.getGivenNameForSearch().isPresent())
                .filter(person -> SearchUtils.matchesGivenName(person.getGivenNameForSearch().get(), givenNameForSearch))
                .filter(person -> person.getParents()
                        .stream()
                        .filter(parent -> parent.getSurnameMainWordForSearch().isPresent()
                                && parent.getGivenNameForSearch().isPresent())
                        .anyMatch(parent -> parent.getSurnameMainWordForSearch().get().equals(fatherSurnameMainWordForSearch)
                                        && SearchUtils.matchesGivenName(parent.getGivenNameForSearch().get(), fatherGivenNameForSearch)
                                || parent.getSurnameMainWordForSearch().get().equals(motherSurnameMainWordForSearch)
                                        && SearchUtils.matchesGivenName(parent.getGivenNameForSearch().get(), motherGivenNameForSearch)))
                .toList();
    }

    public List<EnrichedPerson> findPersonsByNameAndSpouseName(
            String givenName, String surname, SexType sex,
            String spouseGivenName, String spouseSurname,
            EnrichedGedcom gedcom) {

        if (sex == null) {
            return List.of();
        }

        GivenName givenNameForSearch = PersonUtils.getNormalizedGivenNameForSearch(givenName, sex, properties.getNormalizedNamesMap())
                .orElse(null);
        String surnameMainWordForSearch = PersonUtils.getSurnameMainWordForSearch(surname, properties.getNormalizedSurnamesMap())
                .orElse(null);

        if (givenNameForSearch == null || surnameMainWordForSearch == null) {
            return List.of();
        }

        GivenName spouseGivenNameForSearch = PersonUtils.getNormalizedGivenNameForSearch(spouseGivenName, sex, properties.getNormalizedNamesMap())
                .orElse(null);
        String spouseSurnameMainWordForSearch = PersonUtils.getSurnameMainWordForSearch(spouseSurname, properties.getNormalizedSurnamesMap())
                .orElse(null);

        if (spouseGivenNameForSearch == null || spouseSurnameMainWordForSearch == null) {
            return List.of();
        }

        return gedcom
                .getPersonsBySurnameMainWordAndSex(surnameMainWordForSearch, sex)
                .stream()
                .filter(person -> person.getGivenNameForSearch().isPresent())
                .filter(person -> SearchUtils.matchesGivenName(person.getGivenNameForSearch().get(), givenNameForSearch))
                .filter(person -> person.getSpouses()
                        .stream()
                        .filter(spouse -> spouse.getSurnameMainWordForSearch().isPresent()
                                && spouse.getGivenNameForSearch().isPresent())
                        .anyMatch(spouse -> spouse.getSurnameMainWordForSearch().get().equals(spouseSurnameMainWordForSearch)
                                && SearchUtils.matchesGivenName(spouse.getGivenNameForSearch().get(), spouseGivenNameForSearch)))
                .toList();
    }

}
