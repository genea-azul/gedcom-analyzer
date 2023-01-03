package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.ExtensionContainer;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PersonUtils {

    /**
     * Tags taken from EventFact class.
     */
    public static final Set<String> BIRTH_TAGS = Set.of("BIRT", "BIRTH");
    public static final Set<String> BAPTISM_TAGS = Set.of("BAP", "BAPM", "BAPT", "BAPTISM");
    public static final Set<String> CHRISTENING_TAGS = Set.of("CHR", "CHRISTENING");
    public static final Set<String> DEATH_TAGS = Set.of("DEAT", "DEATH");
    public static final Set<String> SEX_TAGS = Set.of("SEX");
    public static final Set<String> EVENT_TAGS = Set.of("EVEN", "EVENT");

    public static final Pattern NAME_SEPARATOR_PATTERN = Pattern.compile("/");

    public static boolean isAlive(Person person) {
        return !isDead(person);
    }

    public static boolean isDead(Person person) {
        return person.getEventsFacts()
                .stream()
                .anyMatch(eventFact -> DEATH_TAGS.contains(eventFact.getTag()));
    }

    public static SexType getSex(Person person) {
        return person.getEventsFacts()
                .stream()
                .filter(eventFact -> SEX_TAGS.contains(eventFact.getTag()))
                .map(EventFact::getValue)
                .map(SexType::valueOf)
                .findFirst()
                .orElse(SexType.U);
    }

    /**
     * The first non-empty display name.
     */
    public static String getDisplayName(Person person) {
        return person.getNames()
                .stream()
                .map(Name::getDisplayValue)
                .map(name -> NAME_SEPARATOR_PATTERN.matcher(name).replaceAll(""))
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("<no name>");
    }

    /**
     * The main name's given name.
     */
    public static Optional<String> getGivenName(Person person) {
        return person.getNames()
                .stream()
                .findFirst()
                .map(Name::getGiven)
                .map(StringUtils::trimToNull);
    }

    public static Optional<String> getGivenNameForSearch(Person person) {
        return getGivenName(person)
                .map(SearchUtils::simplifyName);
    }

    public static Optional<GivenName> getNormalizedGivenNameForSearch(Person person, Map<NameAndSex, String> normalizedNamesMap) {
        return getGivenNameForSearch(person)
                .map(name -> SearchUtils.normalizeName(name, PersonUtils.getSex(person), normalizedNamesMap))
                .map(GivenName::of);
    }

    public static Optional<GivenName> getNormalizedGivenNameForSearch(String givenName, SexType sex, Map<NameAndSex, String> normalizedNamesMap) {
        return Optional.ofNullable(givenName)
                .map(SearchUtils::simplifyName)
                .map(name -> SearchUtils.normalizeName(name, sex, normalizedNamesMap))
                .map(GivenName::of);
    }

    /**
     * The main name's surname.
     */
    public static Optional<String> getSurname(Person person) {
        return person.getNames()
                .stream()
                .findFirst()
                .map(Name::getSurname)
                .map(StringUtils::trimToNull);
    }

    public static Optional<String> getSurnameForSearch(Person person) {
        return getSurname(person)
                .map(SearchUtils::simplifyName);
    }

    public static Optional<String> getSurnameMainWordForSearch(Person person, Map<String, String> normalizedSurnamesMap) {
        return getSurnameForSearch(person)
                .map(surname -> SearchUtils.normalizeSurname(surname, normalizedSurnamesMap));
    }

    public static Optional<String> getSurnameMainWordForSearch(String surname, Map<String, String> normalizedSurnamesMap) {
        return Optional.ofNullable(surname)
                .map(SearchUtils::simplifyName)
                .map(s -> SearchUtils.normalizeSurname(s, normalizedSurnamesMap));
    }

    public static Optional<String> getDateOfBirth(Person person) {
        return person.getEventsFacts()
                .stream()
                .filter(eventFact -> BIRTH_TAGS.contains(eventFact.getTag()))
                .findFirst()
                .or(() -> person.getEventsFacts()
                        .stream()
                        .filter(eventFact -> BAPTISM_TAGS.contains(eventFact.getTag()))
                        .findFirst())
                .or(() -> person.getEventsFacts()
                        .stream()
                        .filter(eventFact -> CHRISTENING_TAGS.contains(eventFact.getTag()))
                        .findFirst())
                .map(EventFact::getDate)
                .map(StringUtils::trimToNull);
    }

    public static Optional<String> getDateOfDeath(Person person) {
        return person.getEventsFacts()
                .stream()
                .filter(eventFact -> DEATH_TAGS.contains(eventFact.getTag()))
                .findFirst()
                .map(EventFact::getDate)
                .map(StringUtils::trimToNull);
    }

    public static Optional<String> getPlaceOfBirth(Person person) {
        return person.getEventsFacts()
                .stream()
                .filter(eventFact -> BIRTH_TAGS.contains(eventFact.getTag()))
                .findFirst()
                .or(() -> person.getEventsFacts()
                        .stream()
                        .filter(eventFact -> BAPTISM_TAGS.contains(eventFact.getTag()))
                        .findFirst())
                .or(() -> person.getEventsFacts()
                        .stream()
                        .filter(eventFact -> CHRISTENING_TAGS.contains(eventFact.getTag()))
                        .findFirst())
                .map(EventFact::getPlace)
                .map(StringUtils::trimToNull);
    }

    public static List<EventFact> getCustomEventFacts(Person person) {
        return person.getEventsFacts()
                .stream()
                .filter(eventFact -> EVENT_TAGS.contains(eventFact.getTag()))
                .toList();
    }

    public static List<GedcomTag> getTagExtensions(Person person) {
        return Stream
                .concat(
                        person.getExtensions()
                                .values()
                                .stream(),
                        person.getEventsFacts()
                                .stream()
                                .map(ExtensionContainer::getExtensions)
                                .map(Map::values)
                                .flatMap(Collection::stream))
                .map(gedcomTags -> (List<?>) gedcomTags)
                .flatMap(List::stream)
                .map(gedcomTag -> (GedcomTag) gedcomTag)
                .filter(gedcomTag -> !gedcomTag.getTag().equals("_UPD"))
                .toList();
    }

    public static List<Person> getParents(Person person, Gedcom gedcom) {
        return person
                .getParentFamilies(gedcom)
                .stream()
                .flatMap(family -> Stream
                        .of(
                                family.getHusbands(gedcom),
                                family.getWives(gedcom))
                        .flatMap(List::stream))
                .toList();
    }

    public static List<Person> getSiblings(Person person, Gedcom gedcom) {
        return person
                .getParentFamilies(gedcom)
                .stream()
                .map(family -> family.getChildren(gedcom))
                .flatMap(List::stream)
                .filter(sibling -> !sibling.getId().equals(person.getId()))
                .toList();
    }

    public static List<Person> getSpouses(Person person, Gedcom gedcom) {
        return person
                .getSpouseFamilies(gedcom)
                .stream()
                .flatMap(family -> Stream
                        .of(
                                family.getHusbands(gedcom),
                                family.getWives(gedcom))
                        .flatMap(List::stream))
                .filter(spouse -> !spouse.getId().equals(person.getId()))
                .toList();
    }

    public static List<Pair<Optional<Person>, List<Person>>> getSpousesWithChildren(Person person, Gedcom gedcom) {
        return person
                .getSpouseFamilies(gedcom)
                .stream()
                .map(family -> Stream
                        .of(
                                family.getHusbands(gedcom),
                                family.getWives(gedcom))
                        .flatMap(List::stream)
                        .map(spouse -> spouse.getId().equals(person.getId()) ? Optional.<Person>empty() : Optional.of(spouse))
                        .map(spouse -> Pair.of(spouse, family.getChildren(gedcom)))
                        .toList())
                .map(spouses -> spouses.size() == 1
                        ? spouses
                        : spouses
                                .stream()
                                .filter(spouseCountPair -> spouseCountPair.getLeft().isPresent())
                                .toList())
                .flatMap(List::stream)
                .toList();
    }

    public static List<Person> getChildren(Person person, Gedcom gedcom) {
        return person
                .getSpouseFamilies(gedcom)
                .stream()
                .map(family -> family.getChildren(gedcom))
                .flatMap(List::stream)
                .toList();
    }

}
