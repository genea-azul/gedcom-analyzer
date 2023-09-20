package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.model.SpouseWithChildren;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.ExtensionContainer;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
                .anyMatch(eventFact
                        -> DEATH_TAGS.contains(eventFact.getTag())
                        || BURIAL_TAGS.contains(eventFact.getTag()));
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

        boolean isGivenNameMissing = StringUtils.isBlank(name.getGiven())
                || "?".equals(StringUtils.trim(name.getGiven()));
        String givenName = isGivenNameMissing ? NO_NAME : name.getGiven();
        displayName = trimAndAppend(displayName, givenName);

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
            @Nullable String givenName,
            @Nullable SexType sex,
            Map<NameAndSex, String> normalizedGivenNamesMap) {
        return Optional.ofNullable(givenName)
                .map(NameUtils::simplifyName)
                .map(simplifiedGivenName -> NameUtils.normalizeGivenName(simplifiedGivenName, sex, normalizedGivenNamesMap))
                .map(normalized -> GivenName.of(givenName, normalized));
    }

    public static Optional<GivenName> getNormalizedGivenName(
            Person person,
            Map<NameAndSex, String> normalizedGivenNamesMap) {
        Optional<String> givenName = getGivenName(person);
        SexType sex = PersonUtils.getSex(person);
        return getNormalizedGivenName(
                givenName.orElse(null),
                sex,
                normalizedGivenNamesMap);
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
            @Nullable String surname,
            Map<String, String> normalizedSurnamesMap) {
        return Optional.ofNullable(surname)
                .map(NameUtils::simplifyName)
                .map(simplifiedSurname -> NameUtils.normalizeSurnameToMainWord(simplifiedSurname, normalizedSurnamesMap))
                .map(normalizedMainWord -> {
                    String shortenedMainWord = NameUtils.shortenSurnameMainWord(normalizedMainWord);
                    return Surname.of(surname, normalizedMainWord, shortenedMainWord);
                });
    }

    public static Optional<Surname> getShortenedSurnameMainWord(
            Person person,
            Map<String, String> normalizedSurnamesMap) {
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
                .map(PlaceUtils::adjustPlace)
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
                .map(PlaceUtils::adjustPlace)
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

    public static List<Pair<String, Optional<ReferenceType>>> getParentsWithReference(Person person, Gedcom gedcom) {
        Map<String, Set<ReferenceType>> references = person
                .getParentFamilyRefs()
                .stream()
                .sorted(ComparatorUtils.transformedComparator(Comparator.nullsFirst(Comparator.naturalOrder()), ParentFamilyRef::getRelationshipType))
                .flatMap(familyRef -> {
                    Family family = familyRef.getFamily(gedcom);
                    ReferenceType referenceType = PersonUtils.resolveParentReferenceType(familyRef.getRelationshipType());

                    return Stream
                            .of(
                                    family.getHusbands(gedcom),
                                    family.getWives(gedcom))
                            .flatMap(List::stream)
                            .map(Person::getId)
                            .map(personId -> Map.entry(personId, referenceType));
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
        return references
                .entrySet()
                .stream()
                .map(entry -> Pair.of(
                        entry.getKey(),
                        entry.getValue().contains(ReferenceType.PARENT)
                                ? Optional.<ReferenceType>empty()
                                : Optional.of(
                                        entry.getValue().contains(ReferenceType.ADOPTIVE_PARENT)
                                                ? ReferenceType.ADOPTIVE_PARENT
                                                : ReferenceType.FOSTER_PARENT)))
                .toList();
    }

    /**
     * Full and half siblings.
     */
    public static List<Person> getAllSiblings(Person person, Gedcom gedcom) {
        return person
                .getParentFamilies(gedcom)
                .stream()
                .flatMap(family -> Stream
                        .of(
                                family.getHusbands(gedcom),
                                family.getWives(gedcom))
                        .flatMap(List::stream)
                        .map(parent -> parent.getSpouseFamilies(gedcom))
                        .flatMap(List::stream)
                        .distinct())
                .map(family -> family.getChildren(gedcom))
                .flatMap(List::stream)
                .filter(sibling -> !sibling.getId().equals(person.getId()))
                // A sibling could be repeated when it has biological and adopted relationship
                .filter(StreamUtils.distinctByKey(Person::getId))
                .toList();
    }

    @SuppressWarnings("unused")
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

    public static List<SpouseWithChildren> getSpousesWithChildren(Person person, Gedcom gedcom) {
        return person
                .getSpouseFamilies(gedcom)
                .stream()
                .map(family -> {
                    List<Pair<String, Optional<ReferenceType>>> children = family
                            .getChildren(gedcom)
                            .stream()
                            .map(child -> Pair.of(
                                    child.getId(),
                                    child
                                            .getParentFamilyRefs()
                                            .stream()
                                            .filter(parentFamilyRef -> parentFamilyRef.getRef().equals(family.getId()))
                                            .findFirst()
                                            .map(ParentFamilyRef::getRelationshipType)
                                            .map(PersonUtils::resolveChildReferenceType)))
                            .toList();
                    boolean isSeparated = FamilyUtils.isSeparated(family);
                    Optional<Date> dateOfPartners = FamilyUtils.getDateOfPartners(family).flatMap(Date::parse);
                    Optional<Date> dateOfSeparation = FamilyUtils.getDateOfSeparation(family).flatMap(Date::parse);
                    Optional<String> placeOfPartners = FamilyUtils.getPlaceOfPartners(family);
                    Optional<String> placeOfSeparation = FamilyUtils.getPlaceOfSeparation(family);
                    return Stream
                            .of(
                                    family.getHusbands(gedcom),
                                    family.getWives(gedcom))
                            .flatMap(List::stream)
                            .map(spouse -> spouse.getId().equals(person.getId())
                                    ? Optional.<Person>empty()
                                    : Optional.of(spouse))
                            .map(spouse -> new SpouseWithChildren(
                                    spouse,
                                    children,
                                    isSeparated,
                                    dateOfPartners,
                                    dateOfSeparation,
                                    placeOfPartners,
                                    placeOfSeparation))
                            .toList();
                })
                // Keep current person as spouse (empty optional) if it is a uni-parental family
                .map(spouses -> spouses.size() == 1
                        ? spouses
                        : spouses
                                .stream()
                                .filter(spouseWithChildren -> spouseWithChildren.spouse().isPresent())
                                .toList())
                .flatMap(List::stream)
                // A spouse should not be repeated.. but just in case..
                .filter(StreamUtils.distinctByKey(spouseWithChildren -> spouseWithChildren
                        .spouse()
                        .map(Person::getId)
                        .orElseGet(() -> String.valueOf(RandomUtils.nextInt()))))
                .toList();
    }

    private static ReferenceType resolveChildReferenceType(@Nullable String relationshipType) {
        if (relationshipType != null) {
            if (FamilyUtils.ADOPTED_CHILD_RELATIONSHIP_TYPES.contains(relationshipType)) {
                return ReferenceType.ADOPTED_CHILD;
            }
            if (FamilyUtils.FOSTER_CHILD_RELATIONSHIP_TYPES.contains(relationshipType)) {
                return ReferenceType.FOSTER_CHILD;
            }
        }
        return ReferenceType.CHILD;
    }

    private static ReferenceType resolveParentReferenceType(@Nullable String relationshipType) {
        if (relationshipType != null) {
            if (FamilyUtils.ADOPTED_CHILD_RELATIONSHIP_TYPES.contains(relationshipType)) {
                return ReferenceType.ADOPTIVE_PARENT;
            }
            if (FamilyUtils.FOSTER_CHILD_RELATIONSHIP_TYPES.contains(relationshipType)) {
                return ReferenceType.FOSTER_PARENT;
            }
        }
        return ReferenceType.PARENT;
    }

    @SuppressWarnings("unused")
    public static List<Person> getChildren(Person person, Gedcom gedcom) {
        return person
                .getSpouseFamilies(gedcom)
                .stream()
                .map(family -> family.getChildren(gedcom))
                .flatMap(List::stream)
                // A child could be repeated when it has biological and adopted relationship with a parent
                .filter(StreamUtils.distinctByKey(Person::getId))
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

    public static List<SexType> getOrValidValues(@Nullable SexType sex) {
        if (sex != null) {
            return List.of(sex);
        }
        return SexType.VALID_SEX_TYPES;
    }

    public static final Comparator<EnrichedPerson> DATES_COMPARATOR = Comparator
            .nullsLast((p1, p2) -> {
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
            });

}
