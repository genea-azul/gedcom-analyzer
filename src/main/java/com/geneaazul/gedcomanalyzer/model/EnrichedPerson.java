package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.utils.FamilyUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;
import com.geneaazul.gedcomanalyzer.utils.PlaceUtils;

import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Person;

import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import lombok.Getter;

@Getter
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
public class EnrichedPerson {

    private final Person person;
    private final EnrichedGedcom gedcom;
    private final GedcomAnalyzerProperties properties;

    private final String id;
    private final SexType sex;
    private final Optional<String> surname;
    private final String displayName;
    private final Optional<String> aka;
    private final Optional<Date> dateOfBirth;
    private final Optional<Date> dateOfDeath;
    private final Optional<String> placeOfBirth;
    private final boolean isAlive;
    private final Optional<Age> age;

    // Search values
    private final Optional<GivenName> givenNameForSearch;
    private final Optional<String> surnameForSearch;
    private final Optional<String> surnameMainWordForSearch;
    private final Optional<String> placeOfBirthForSearch;
    private final Optional<String> countryOfBirthForSearch;

    // Custom event facts and tag extensions
    private List<EventFact> customEventFacts;
    private List<EventFact> familyCustomEventFacts;
    private List<GedcomTag> tagExtensions;

    // Enriched family
    private List<EnrichedPerson> parents;
    private List<EnrichedPerson> siblings;
    private List<EnrichedPerson> spouses;
    private List<EnrichedPerson> children;

    private EnrichedPerson(Person person, EnrichedGedcom gedcom) {
        this.person = person;
        this.gedcom = gedcom;
        this.properties = gedcom.getProperties();

        id = person.getId();
        sex = PersonUtils.getSex(person);
        surname = PersonUtils.getSurname(person);
        displayName = PersonUtils.getDisplayName(person);
        aka = PersonUtils.getAka(person)
                .filter(akaName -> !akaName.equals(displayName));
        dateOfBirth = PersonUtils.getDateOfBirth(person).flatMap(Date::parse);
        dateOfDeath = PersonUtils.getDateOfDeath(person).flatMap(Date::parse);
        placeOfBirth = PersonUtils.getPlaceOfBirth(person);
        isAlive = PersonUtils.isAlive(person);
        age = Age.of(dateOfBirth, dateOfDeath
                .or(() -> isAlive ? Optional.of(Date.now(properties.getZoneId())) : Optional.empty()));

        givenNameForSearch = PersonUtils.getNormalizedGivenNameForSearch(person, properties.getNormalizedNamesMap());
        surnameForSearch = PersonUtils.getSurnameForSearch(person);
        surnameMainWordForSearch = PersonUtils.getSurnameMainWordForSearch(person, properties.getNormalizedSurnamesMap());
        placeOfBirthForSearch = placeOfBirth
                .map(PlaceUtils::removeLastParenthesis);
        countryOfBirthForSearch = placeOfBirthForSearch
                .map(PlaceUtils::getCountry);
    }

    public static EnrichedPerson of(Person person, EnrichedGedcom gedcom) {
        return new EnrichedPerson(person, gedcom);
    }

    public void enrichFamily(Map<String, EnrichedPerson> enrichedPeopleIndex) {
        parents = toEnrichedPeople(PersonUtils.getParents(person, gedcom.getGedcom()), enrichedPeopleIndex, null);
        siblings = toEnrichedPeople(PersonUtils.getSiblings(person, gedcom.getGedcom()), enrichedPeopleIndex, PersonUtils.DATES_COMPARATOR);
        spouses = toEnrichedPeople(PersonUtils.getSpouses(person, gedcom.getGedcom()), enrichedPeopleIndex, PersonUtils.DATES_COMPARATOR);
        children = toEnrichedPeople(PersonUtils.getChildren(person, gedcom.getGedcom()), enrichedPeopleIndex, PersonUtils.DATES_COMPARATOR);
    }

    private List<EnrichedPerson> toEnrichedPeople(
            List<Person> people,
            Map<String, EnrichedPerson> enrichedPeopleIndex,
            @Nullable Comparator<EnrichedPerson> personComparator) {

        Stream<EnrichedPerson> peopleStream = people
                .stream()
                .map(Person::getId)
                .map(enrichedPeopleIndex::get);

        if (personComparator != null) {
            peopleStream = peopleStream
                    .sorted(personComparator);
        }

        return peopleStream
                .toList();
    }

    public List<Pair<Optional<EnrichedPerson>, List<EnrichedPerson>>> getSpousesWithChildren() {
        return toEnrichedSpousesWithChildren(
                PersonUtils.getSpousesWithChildren(person, gedcom.getGedcom()),
                gedcom.getPeopleByIdIndex(),
                PersonUtils.DATES_COMPARATOR);
    }

    private List<Pair<Optional<EnrichedPerson>, List<EnrichedPerson>>> toEnrichedSpousesWithChildren(
            List<Pair<Optional<Person>, List<Person>>> spousesWithChildren,
            Map<String, EnrichedPerson> enrichedPeopleIndex,
            Comparator<EnrichedPerson> personComparator) {
        return spousesWithChildren
                .stream()
                .map(pair -> Pair.of(
                        pair.getLeft()
                                .map(Person::getId)
                                .map(enrichedPeopleIndex::get),
                        toEnrichedPeople(
                                pair.getRight(),
                                enrichedPeopleIndex,
                                personComparator)))
                .sorted((pair1, pair2) -> {
                    EnrichedPerson p1 = pair1.getLeft().orElse(null);
                    EnrichedPerson p2 = pair2.getLeft().orElse(null);
                    return ComparatorUtils.nullHighComparator(personComparator).compare(p1, p2);
                })
                .toList();
    }

    public void analyzeCustomEventFactsAndTagExtensions() {
        customEventFacts = PersonUtils.getCustomEventFacts(person);
        familyCustomEventFacts =  person.getSpouseFamilies(gedcom.getGedcom())
                .stream()
                .map(FamilyUtils::getCustomEventFacts)
                .flatMap(List::stream)
                .toList();
        tagExtensions = PersonUtils.getTagExtensions(person);
    }

    public boolean equalsSex(EnrichedPerson other) {
        return this.sex != SexType.U && this.sex == other.sex;
    }

    public boolean matchesGivenName(EnrichedPerson other) {
        return this.givenNameForSearch.isPresent()
                && other.givenNameForSearch.isPresent()
                && this.givenNameForSearch.get().matches(other.givenNameForSearch.get());
    }

    public boolean matchesSurname(EnrichedPerson other) {
        return this.surnameMainWordForSearch.isPresent()
                && other.surnameMainWordForSearch.isPresent()
                && this.surnameMainWordForSearch.get().equals(other.surnameMainWordForSearch.get());
    }

    public boolean matchesParents(EnrichedPerson other) {
        return matchesPersons(this.getParents(), other.getParents(), true);
    }

    public boolean matchesSpouses(EnrichedPerson other) {
        return matchesPersons(this.getSpouses(), other.getSpouses(), false);
    }

    private static boolean matchesPersons(List<EnrichedPerson> persons1, List<EnrichedPerson> persons2, boolean isAllMatch) {
        if (isAllMatch && persons1.size() > persons2.size()) {
            return false;
        }

        List<Pair<GivenName, String>> givenAndSurnamePairs2 = persons2
                .stream()
                .map(person -> Pair.of(person.givenNameForSearch, person.surnameMainWordForSearch))
                .filter(pair -> pair.getLeft().isPresent() && pair.getRight().isPresent())
                .map(pair -> Pair.of(pair.getLeft().get(), pair.getRight().get()))
                .toList();

        if (isAllMatch && persons1.size() > givenAndSurnamePairs2.size()) {
            return false;
        }

        List<Pair<GivenName, String>> givenAndSurnamePairs1 = persons1
                .stream()
                .map(person -> Pair.of(person.givenNameForSearch, person.surnameMainWordForSearch))
                .filter(pair -> pair.getLeft().isPresent() && pair.getRight().isPresent())
                .map(pair -> Pair.of(pair.getLeft().get(), pair.getRight().get()))
                .toList();

        if (givenAndSurnamePairs1.isEmpty() || isAllMatch && persons1.size() > givenAndSurnamePairs1.size()) {
            return false;
        }

        Predicate<Pair<GivenName, String>> matcher = pair -> givenAndSurnamePairs2
                .stream()
                .anyMatch(otherPair
                        -> pair.getRight().equals(otherPair.getRight())
                        && pair.getLeft().matches(otherPair.getLeft()));

        return isAllMatch
                ? givenAndSurnamePairs1
                        .stream()
                        .allMatch(matcher)
                : givenAndSurnamePairs1
                        .stream()
                        .anyMatch(matcher);
    }

    public boolean equalsDateOfBirthByDay(EnrichedPerson other) {
        return matchesByDay(
                this.getDateOfBirth().orElse(null),
                other.getDateOfBirth().orElse(null),
                Period.ZERO);
    }

    public boolean matchesDateOfBirthByDay(EnrichedPerson other) {
        return matchesByDay(
                this.getDateOfBirth().orElse(null),
                other.getDateOfBirth().orElse(null),
                properties.getMatchByDayMaxPeriod());
    }

    public boolean equalsDateOfDeathByDay(EnrichedPerson other) {
        return matchesByDay(
                this.getDateOfDeath().orElse(null),
                other.getDateOfDeath().orElse(null),
                Period.ZERO);
    }

    public boolean matchesDateOfDeathByDay(EnrichedPerson other) {
        return matchesByDay(
                this.getDateOfDeath().orElse(null),
                other.getDateOfDeath().orElse(null),
                properties.getMatchByDayMaxPeriod());
    }

    public boolean matchesDateOfBirthByMonth(EnrichedPerson other) {
        return matchesByMonth(
                this.getDateOfBirth().orElse(null),
                other.getDateOfBirth().orElse(null),
                properties.getMatchByMonthMaxPeriod());
    }

    public boolean matchesDateOfDeathByMonth(EnrichedPerson other) {
        return matchesByMonth(
                this.getDateOfDeath().orElse(null),
                other.getDateOfDeath().orElse(null),
                properties.getMatchByMonthMaxPeriod());
    }

    public boolean matchesDateOfBirthByYear(EnrichedPerson other) {
        return this.getDateOfBirth().isPresent()
                && other.getDateOfBirth().isPresent()
                && this.getDateOfBirth().get().isCloseToByYear(other.getDateOfBirth().get(), properties.getMatchByYearMaxPeriod());
    }

    public boolean matchesDateOfDeathByYear(EnrichedPerson other) {
        return this.getDateOfDeath().isPresent()
                && other.getDateOfDeath().isPresent()
                && this.getDateOfDeath().get().isCloseToByYear(other.getDateOfDeath().get(), properties.getMatchByYearMaxPeriod());
    }

    public boolean matchesDateOfBirthByAny(EnrichedPerson other) {
        return matchesDateOfBirthByDay(other)
                || matchesDateOfBirthByMonth(other)
                || matchesDateOfBirthByYear(other);
    }

    public boolean matchesDateOfDeathByAny(EnrichedPerson other) {
        return matchesDateOfDeathByDay(other)
                || matchesDateOfDeathByMonth(other)
                || matchesDateOfDeathByYear(other);
    }

    private boolean matchesByDay(Date date1, Date date2, Period delta) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isCloseToByDay(date2, delta);
    }

    private boolean matchesByMonth(Date date1, Date date2, Period delta) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isCloseToByMonth(date2, delta);
    }

    public String format() {
        return StringUtils.rightPad(id, 8)
                + " - " + sex
                + " " + (isAlive ? " " : "X")
                + " - " + StringUtils.rightPad(displayName, 36)
                + " - " + StringUtils.leftPad(dateOfBirth.map(Date::format).orElse(""), 11)
                + " - " + StringUtils.leftPad(dateOfDeath.map(Date::format).orElse(""), 11)
                + " - " + StringUtils.rightPad(parents
                        .stream()
                        .map(EnrichedPerson::getDisplayName)
                        .collect(Collectors.joining(", ")), 64)
                + " - " + getSpousesWithChildren()
                        .stream()
                        .map(spouseCountPair -> spouseCountPair.getLeft()
                                .map(EnrichedPerson::getDisplayName)
                                .orElse("<no spouse>") + " (" + spouseCountPair.getRight().size() + ")")
                        .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return format();
    }

}
