package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.ExtensionContainer;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PersonUtils {

    public static final String PRIVATE_NAME = "<private>";
    public static final String NO_NAME = "<no name>";
    public static final String NO_SPOUSE = "<no spouse>";

    /**
     * Tags taken from EventFact class.
     */
    public static final Set<String> BIRTH_TAGS = Set.of("BIRT", "BIRTH");
    public static final Set<String> BAPTISM_TAGS = Set.of("BAP", "BAPM", "BAPT", "BAPTISM");
    public static final Set<String> CHRISTENING_TAGS = Set.of("CHR", "CHRISTENING");
    public static final Set<String> DEATH_TAGS = Set.of("DEAT", "DEATH");
    public static final Set<String> BURIAL_TAGS = Set.of("BURI", "BURIAL");
    public static final Set<String> SEX_TAGS = Set.of("SEX");
    public static final Set<String> EVENT_TAGS = Set.of("EVEN", "EVENT");

    /**
     * Custom extension tags.
     */
    public static final String FORMER_NAME_TAG = "_FORMERNAME";
    public static final String UPDATED_TAG = "_UPD";

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
                .map(PersonUtils::buildDisplayName)
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(NO_NAME);
    }

    private static String buildDisplayName(Name name) {
        String displayName = "";
        if (StringUtils.isNotBlank(name.getPrefix())) {
            displayName = trimAndAppend(displayName, name.getPrefix());
        }
        if (StringUtils.isNotBlank(name.getGiven())) {
            String givenName = "?".equals(StringUtils.trim(name.getGiven())) ? NO_NAME : name.getGiven();
            displayName = trimAndAppend(displayName, givenName);
        }
        if (StringUtils.isNotBlank(name.getNickname())) {
            displayName = trimAndAppend(displayName, "\"" + name.getNickname() + "\"");
        }
        if (StringUtils.isNotBlank(name.getSurnamePrefix())) {
            displayName = trimAndAppend(displayName, name.getSurnamePrefix());
        }
        if (StringUtils.isNotBlank(name.getSurname())) {
            displayName = trimAndAppend(displayName, name.getSurname());
        }
        if (StringUtils.isNotBlank(name.getSuffix())) {
            displayName = trimAndAppend(displayName, name.getSuffix());
        }
        return displayName;
    }

    private static String trimAndAppend(String str, String append) {
        append = append.trim();
        return StringUtils.isEmpty(str) ? append : str + " " + append;
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

    public static Optional<GivenName> getNormalizedGivenName(
            @Nullable String givenName, SexType sex, Map<NameAndSex, String> normalizedGivenNamesMap) {
        return Optional.ofNullable(givenName)
                .map(SearchUtils::simplifyName)
                .map(simplifiedGivenName -> SearchUtils.normalizeGivenName(simplifiedGivenName, sex, normalizedGivenNamesMap))
                .map(normalized -> GivenName.of(givenName, normalized));
    }

    public static Optional<GivenName> getNormalizedGivenName(
            Person person, SexType sex, Map<NameAndSex, String> normalizedGivenNamesMap) {
        Optional<String> givenName = getGivenName(person);
        return getNormalizedGivenName(givenName.orElse(null), sex, normalizedGivenNamesMap);
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

    public static Optional<Surname> getShortenedSurnameMainWord(
            @Nullable String surname, Map<String, String> normalizedSurnamesMap) {
        return Optional.ofNullable(surname)
                .map(SearchUtils::simplifyName)
                .map(simplifiedSurname -> SearchUtils.normalizeSurnameToMainWord(simplifiedSurname, normalizedSurnamesMap))
                .map(normalizedMainWord -> {
                    String shortenedMainWord = SearchUtils.shortenSurnameMainWord(normalizedMainWord);
                    return Surname.of(surname, normalizedMainWord, shortenedMainWord);
                });
    }

    public static Optional<Surname> getShortenedSurnameMainWord(
            Person person, Map<String, String> normalizedSurnamesMap) {
        Optional<String> surname = getSurname(person);
        return getShortenedSurnameMainWord(surname.orElse(null), normalizedSurnamesMap);
    }

    /**
     * The main name's a.k.a. name (or former name).
     */
    public static Optional<String> getAka(Person person) {
        Optional<Name> mainName = person.getNames()
                .stream()
                .findFirst();
        return mainName
                .map(Name::getAka)
                .or(() -> mainName
                        .map(Name::getExtensions)
                        .map(Map::values)
                        .map(gedcomTags -> (Collection<?>) gedcomTags)
                        .flatMap(gedcomTagsList -> gedcomTagsList
                                .stream()
                                .map(gedcomTags -> (List<?>) gedcomTags)
                                .flatMap(Collection::stream)
                                .map(gedcomTag -> (GedcomTag) gedcomTag)
                                .filter(gedcomTag -> gedcomTag.getTag().equals(FORMER_NAME_TAG))
                                .findFirst())
                        .map(GedcomTag::getValue))
                .map(StringUtils::trimToNull);
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
                .or(() -> person.getEventsFacts()
                        .stream()
                        .filter(eventFact -> BURIAL_TAGS.contains(eventFact.getTag()))
                        .findFirst())
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

    public static Optional<String> getPlaceOfDeath(Person person) {
        return person.getEventsFacts()
                .stream()
                .filter(eventFact -> DEATH_TAGS.contains(eventFact.getTag()))
                .findFirst()
                .or(() -> person.getEventsFacts()
                        .stream()
                        .filter(eventFact -> BURIAL_TAGS.contains(eventFact.getTag()))
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
                .of(
                        person.getExtensions()
                                .values()
                                .stream(),
                        person.getNames()
                                .stream()
                                .map(ExtensionContainer::getExtensions)
                                .map(Map::values)
                                .flatMap(Collection::stream),
                        person.getEventsFacts()
                                .stream()
                                .map(ExtensionContainer::getExtensions)
                                .map(Map::values)
                                .flatMap(Collection::stream))
                .flatMap(Function.identity())
                .map(gedcomTags -> (List<?>) gedcomTags)
                .flatMap(List::stream)
                .map(gedcomTag -> (GedcomTag) gedcomTag)
                .filter(gedcomTag -> !gedcomTag.getTag().equals(UPDATED_TAG))
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
                // Keep current person as spouse (empty optional) if it is a uni-parental family
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

    public static String obfuscateName(
            EnrichedPerson person,
            boolean condition) {
        return condition
                ? person.getSurname()
                        .map(Surname::value)
                        .map(surname -> PRIVATE_NAME + " " + surname)
                        .orElse(PRIVATE_NAME)
                : person.getDisplayName();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static String obfuscateSpouseName(
            Optional<EnrichedPerson> maybeSpouse,
            Predicate<EnrichedPerson> condition) {
        return maybeSpouse
                .map(spouse -> obfuscateName(spouse, condition.test(spouse)))
                .orElse(NO_SPOUSE);
    }

    public static final Comparator<EnrichedPerson> DATES_COMPARATOR = (p1, p2) -> {
        Date dob1 = p1.getDateOfBirth().orElse(null);
        Date dob2 = p2.getDateOfBirth().orElse(null);
        int cmp = ObjectUtils.compare(dob1, dob2, true);
        if (cmp != 0) {
            return cmp;
        }
        Date dod1 = p1.getDateOfDeath().orElse(null);
        Date dod2 = p2.getDateOfDeath().orElse(null);
        cmp = ObjectUtils.compare(dod1, dod2, true);
        if (cmp != 0) {
            return cmp;
        }
        return p1.getId().compareTo(p2.getId());
    };

}
