package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.utils.FamilyUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;
import com.geneaazul.gedcomanalyzer.utils.StreamUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Person;

import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

@Getter
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
public class EnrichedPerson {

    // Constructor properties
    private final Person legacyPerson;
    private final EnrichedGedcom gedcom;
    private final GedcomAnalyzerProperties properties;

    private final String id;
    private final UUID uuid;
    private final SexType sex;
    private final Optional<GivenName> givenName;
    private final Optional<Surname> surname;
    private final String displayName;
    private final Optional<String> aka;
    private final Optional<ProfilePicture> profilePicture;
    private final Optional<Date> dateOfBirth;
    private final Optional<Date> dateOfDeath;
    private final Optional<Place> placeOfBirth;
    private final Optional<Place> placeOfDeath;
    private final boolean isAlive;
    private final Optional<Age> age;
    private final boolean isDistinguishedPerson;

    // Custom event facts and tag extensions
    private List<EventFact> customEventFacts;
    private List<EventFact> familyCustomEventFacts;
    private List<GedcomTag> tagExtensions;

    // Enriched family
    private List<EnrichedPersonWithReference> parentsWithReference;
    private List<EnrichedSpouseWithChildren> spousesWithChildren;
    private List<EnrichedPerson> parents;
    private List<EnrichedPerson> allSiblings;
    private List<EnrichedPerson> spouses;
    private List<EnrichedPerson> children;

    // Transient properties
    @Setter
    private Integer personsCountInTree;
    @Setter
    private Integer surnamesCountInTree;
    @Setter
    private List<String> ancestryCountries;
    @Setter
    private AncestryGenerations ancestryGenerations;
    @Setter
    private Optional<Relationship> maxDistantRelationship;
    @Setter
    private List<EnrichedPerson> distinguishedPersonsInTree;
    @Setter
    private Integer orderKey;

    private EnrichedPerson(Person person, EnrichedGedcom gedcom) {
        this.properties = gedcom.getProperties();
        this.legacyPerson = properties.isKeepReferenceToLegacyGedcom() ? person : null;
        this.gedcom = gedcom;

        id = person.getId();
        uuid = UUID.randomUUID();
        sex = PersonUtils.getSex(person);
        givenName = PersonUtils.getNormalizedGivenName(person, properties.getNormalizedGivenNamesMap());
        surname = PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap());
        displayName = PersonUtils.getDisplayName(person);
        aka = PersonUtils.getAka(person)
                .filter(akaName -> !akaName.equals(displayName));
        profilePicture = PersonUtils.getProfilePicture(person);
        dateOfBirth = PersonUtils.getDateOfBirth(person)
                .flatMap(Date::parse);
        dateOfDeath = PersonUtils.getDateOfDeath(person)
                .flatMap(Date::parse);
        placeOfBirth = PersonUtils.getPlaceOfBirth(person)
                .map(place -> Place.of(place, gedcom.getPlaces()));
        placeOfDeath = PersonUtils.getPlaceOfDeath(person)
                .map(place -> Place.of(place, gedcom.getPlaces()));
        isAlive = PersonUtils.isAlive(person);
        age = Age.of(dateOfBirth, dateOfDeath
                .or(() -> isAlive ? Optional.of(Date.now(properties.getZoneId())) : Optional.empty()));
        isDistinguishedPerson = PersonUtils.isDistinguishedPerson(person);
    }

    public static EnrichedPerson of(Person legacyPerson, EnrichedGedcom gedcom) {
        return new EnrichedPerson(legacyPerson, gedcom);
    }

    public Optional<Person> getLegacyPerson() {
        return Optional.ofNullable(legacyPerson);
    }

    public void enrichFamily(Gedcom legacyGedcom, Map<String, EnrichedPerson> enrichedPeopleIndex) {
        Person legacyPerson = legacyGedcom.getPerson(id);
        parentsWithReference = toEnrichedPeopleWithReference(PersonUtils.getParentsWithReference(legacyPerson, legacyGedcom), enrichedPeopleIndex, null);
        spousesWithChildren = toEnrichedSpousesWithChildren(PersonUtils.getSpousesWithChildren(legacyPerson, legacyGedcom, gedcom.getPlaces()), enrichedPeopleIndex);
        allSiblings = toEnrichedPeople(PersonUtils.getAllSiblings(legacyPerson, legacyGedcom), enrichedPeopleIndex, PersonUtils.DATES_COMPARATOR);

        parents = parentsWithReference
                .stream()
                .map(EnrichedPersonWithReference::person)
                // A parent could be repeated when it has biological and adopted relationship with a child
                .filter(StreamUtils.distinctByKey(EnrichedPerson::getId))
                .toList();
        spouses = spousesWithChildren
                .stream()
                .map(EnrichedSpouseWithChildren::getSpouse)
                .flatMap(Optional::stream)
                .toList();
        children = spousesWithChildren
                .stream()
                .map(EnrichedSpouseWithChildren::getChildren)
                .flatMap(List::stream)
                // A child could be repeated when it has biological and adopted relationship with a parent
                .filter(StreamUtils.distinctByKey(EnrichedPerson::getId))
                .toList();
    }

    private List<EnrichedPerson> toEnrichedPeople(
            List<Person> people,
            Map<String, EnrichedPerson> enrichedPeopleIndex,
            @SuppressWarnings("SameParameterValue") @Nullable Comparator<EnrichedPerson> personComparator) {

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

    private List<EnrichedSpouseWithChildren> toEnrichedSpousesWithChildren(
            List<SpouseWithChildren> spousesWithChildren,
            Map<String, EnrichedPerson> enrichedPeopleIndex) {
        return spousesWithChildren
                .stream()
                .map(spouseWithChildren -> EnrichedSpouseWithChildren.of(
                        spouseWithChildren
                                .spouse()
                                .map(Person::getId)
                                .map(enrichedPeopleIndex::get),
                        toEnrichedPeopleWithReference(
                                spouseWithChildren.children(),
                                enrichedPeopleIndex,
                                PersonUtils.DATES_COMPARATOR),
                        spouseWithChildren.isSeparated(),
                        spouseWithChildren.dateOfPartners(),
                        spouseWithChildren.dateOfSeparation(),
                        spouseWithChildren.placeOfPartners(),
                        spouseWithChildren.placeOfSeparation()))
                .sorted(FamilyUtils.DATES_COMPARATOR)
                .toList();
    }

    private List<EnrichedPersonWithReference> toEnrichedPeopleWithReference(
            List<Pair<String, Optional<ReferenceType>>> people,
            Map<String, EnrichedPerson> enrichedPeopleIndex,
            @Nullable Comparator<EnrichedPerson> personComparator) {

        Stream<EnrichedPersonWithReference> peopleStream = people
                .stream()
                .map(personWithReference -> new EnrichedPersonWithReference(
                        enrichedPeopleIndex.get(personWithReference.getLeft()),
                        personWithReference.getRight()));

        if (personComparator != null) {
            peopleStream = peopleStream
                    .sorted((p1, p2) -> personComparator.compare(p1.person(), p2.person()));
        }

        return peopleStream
                .toList();
    }

    public List<Place> getPlacesOfAnyEvent() {
        return Stream.concat(
                Stream
                        .of(
                                this.placeOfBirth,
                                this.placeOfDeath),
                this.spousesWithChildren
                        .stream()
                        .flatMap(spouseWithChildren -> Stream
                                .of(
                                        spouseWithChildren.getPlaceOfPartners(),
                                        spouseWithChildren.getPlaceOfSeparation())))
                .flatMap(Optional::stream)
                .distinct()
                .toList();
    }

    public void analyzeCustomEventFactsAndTagExtensions(Gedcom gedcom) {
        getLegacyPerson()
                .ifPresent(person -> {
                    customEventFacts = PersonUtils.getCustomEventFacts(person);
                    familyCustomEventFacts =  person.getSpouseFamilies(gedcom)
                            .stream()
                            .map(FamilyUtils::getCustomEventFacts)
                            .flatMap(List::stream)
                            .toList();
                    tagExtensions = PersonUtils.getTagExtensions(person);
                });
    }

    public boolean equalsSex(EnrichedPerson other) {
        return this.sex != SexType.U && this.sex == other.sex;
    }

    public boolean matchesGivenNameAndSurname(EnrichedPerson other) {
        return matchesGivenNameAndSurname(GivenNameAndSurname.of(other.givenName, other.surname, other.aka));
    }

    public boolean matchesGivenNameAndSurname(GivenNameAndSurname givenNameAndSurname) {
        return GivenNameAndSurname.of(this.givenName, this.surname, this.aka).matches(givenNameAndSurname);
    }

    public boolean matchesOptionalGivenNameAndSurname(EnrichedPerson other) {
        return matchesOptionalGivenNameAndSurname(GivenNameAndSurname.of(other.givenName, other.surname, other.aka));
    }

    public boolean matchesOptionalGivenNameAndSurname(GivenNameAndSurname givenNameAndSurname) {
        return GivenNameAndSurname.of(this.givenName, this.surname, this.aka).matchesWithOptionalGivenName(givenNameAndSurname);
    }

    public boolean matchesAllParents(EnrichedPerson other) {
        return matchesPersonsBySexAndName(this.parents, other.parents, true, false);
    }

    public boolean matchesAnySpouses(EnrichedPerson other) {
        return matchesPersonsBySexAndName(this.spouses, other.spouses, false, false);
    }

    public boolean matchesAnySpousesWithOptionalGivenName(EnrichedPerson other) {
        return matchesPersonsBySexAndName(this.spouses, other.spouses, false, true);
    }

    private static boolean matchesPersonsBySexAndName(
            List<EnrichedPerson> persons1,
            List<EnrichedPerson> persons2,
            boolean isAllMatch,
            boolean isOptionalGivenNameMatch) {

        if (persons1.isEmpty() || persons2.isEmpty() || isAllMatch && persons1.size() > persons2.size()) {
            return false;
        }

        Predicate<EnrichedPerson> matcher = person1 -> persons2
                .stream()
                .anyMatch(person2
                        -> person1.equalsSex(person2)
                        && (!isOptionalGivenNameMatch && person1.matchesGivenNameAndSurname(person2)
                                || isOptionalGivenNameMatch && person1.matchesOptionalGivenNameAndSurname(person2)));

        return isAllMatch
                ? persons1
                        .stream()
                        .allMatch(matcher)
                : persons1
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

    private boolean matchesByMonth(@Nullable Date date1, @Nullable Date date2, Period delta) {
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
                + " - " + StringUtils.rightPad(placeOfBirth.map(place -> StringUtils.substring(place.name(), 0, 36)).orElse(""), 36)
                + " - " + StringUtils.rightPad(placeOfDeath.map(place -> StringUtils.substring(place.name(), 0, 36)).orElse(""), 36)
                + " - " + StringUtils.rightPad(parents
                        .stream()
                        .map(EnrichedPerson::getDisplayName)
                        .collect(Collectors.joining(", ")), 64)
                + " - " + getSpousesWithChildren()
                        .stream()
                        .map(spouseWithChildren -> spouseWithChildren.getSpouse()
                                .map(EnrichedPerson::getDisplayName)
                                .orElse(PersonUtils.NO_SPOUSE) + " (" + spouseWithChildren.getChildren().size() + ")")
                        .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return format();
    }

}
