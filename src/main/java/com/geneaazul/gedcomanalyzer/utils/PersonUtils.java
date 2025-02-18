package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.Age;
import com.geneaazul.gedcomanalyzer.model.Aka;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.ProfilePicture;
import com.geneaazul.gedcomanalyzer.model.SpouseWithChildren;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.text.StringEscapeUtils;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.ExtensionContainer;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    public static final Set<String> CHRISTENING_TAGS = Set.of("CHR", "CHRA", "CHRISTENING");
    public static final Set<String> DEATH_TAGS = Set.of("DEAT", "DEATH");
    public static final Set<String> BURIAL_TAGS = Set.of("BURI", "BURIAL");
    public static final Set<String> SEX_TAGS = Set.of("SEX");
    public static final Set<String> RESI_TAGS = Set.of("RESI");
    public static final Set<String> EVENT_TAGS = Set.of("EVEN", "EVENT");
    public static final Set<String> ETHNICITY_EVENT_TYPES = Set.of("ETHN", "ETHNICITY", "Grupo étnico");
    public static final Set<String> DISAPPEARED_PERSON_EVENT_TYPES = Set.of("Enforced disappearance", "Desaparición forzada");
    public static final Set<String> COMMENT_EVENT_TYPES = Set.of("Comment");
    public static final Set<String> DISTINGUISHED_PERSON_COMMENT_VALUES = Set.of("Personalidad destacada");
    public static final Set<String> NATIVE_PERSON_COMMENT_VALUES = Set.of("Pueblos originarios");

    /**
     * Custom extension tags.
     */
    public static final String FORMER_NAME_TAG = "_FORMERNAME";
    public static final String UPDATED_TAG = "_UPD";
    public static final String PERSONAL_PHOTO_TAG = "_PERSONALPHOTO";
    public static final String EMAIL_TAG = "EMAIL";

    public static final Pattern UPDATE_DATE_PATTERN = Pattern.compile("(\\d{1,2}) (\\w{3}) (\\d{4}) (.{8}) (GMT|UTC)\\s*([+-]\\d{4})");
    public static final DateTimeFormatter UPDATE_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private static final UniformRandomProvider RNG = RandomSource.JDK.create();

    public static Integer getId(Person person) {
        return Optional.of(person.getId())
                .map(id -> id.startsWith("I") ? id.substring(1) : id)
                .filter(StringUtils::isNumeric)
                .map(Integer::valueOf)
                .orElseGet(RNG::nextInt);
    }

    public static UUID getUuid(
            Integer id,
            @Nullable ZonedDateTime modifiedDateTime) {

        long offset = Optional.ofNullable(modifiedDateTime)
                .filter(mdt -> mdt.getNano() != 0)
                .map(mdt -> mdt.getNano() / 1_000L)
                .orElse(1L);
        long personId = id * offset;

        long timestamp = Optional.ofNullable(modifiedDateTime)
                .map(ZonedDateTime::toEpochSecond)
                .orElseGet(RNG::nextLong);

        return new UUID(personId, timestamp);
    }

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

    public static boolean isDistinguishedPerson(Person person) {
        return person.getEventsFacts()
                .stream()
                .anyMatch(eventFact
                        -> EVENT_TAGS.contains(eventFact.getTag())
                        && COMMENT_EVENT_TYPES.contains(eventFact.getType())
                        && DISTINGUISHED_PERSON_COMMENT_VALUES.contains(eventFact.getValue()));
    }

    public static boolean isNativePerson(Person person) {
        return person.getEventsFacts()
                .stream()
                .anyMatch(eventFact
                        -> EVENT_TAGS.contains(eventFact.getTag())
                        && ETHNICITY_EVENT_TYPES.contains(eventFact.getType())
                        && NATIVE_PERSON_COMMENT_VALUES.contains(eventFact.getValue()));
    }

    public static boolean isDisappearedPerson(Person person) {
        return person.getEventsFacts()
                .stream()
                .anyMatch(eventFact
                        -> EVENT_TAGS.contains(eventFact.getTag())
                        && DISAPPEARED_PERSON_EVENT_TYPES.contains(eventFact.getType()));
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
            displayName = trimAndAppend(displayName, name.getSuffix(), ", ");
        }
        return displayName;
    }

    public static String getDistinguishedPersonNameForSite(
            Person person,
            Map<String, String> namePrefixes) {
        return person.getNames()
                .stream()
                .map(name -> {
                    String displayName = "";
                    if (StringUtils.isNotBlank(name.getPrefix())) {
                        @Nullable String simplified = NameUtils.simplifyName(name.getPrefix());
                        @Nullable String abbreviation = namePrefixes.get(simplified);
                        if (abbreviation != null) {
                            displayName = trimAndAppend(displayName, "<span class=\"text-secondary\"><abbr title=\"" + StringEscapeUtils.escapeHtml4(abbreviation) + "\" tabindex=\"0\">" + StringEscapeUtils.escapeHtml4(name.getPrefix()) + "</abbr></span>");
                        } else {
                            displayName = trimAndAppend(displayName, "<span class=\"text-secondary\">" + StringEscapeUtils.escapeHtml4(name.getPrefix()) + "</span>");
                        }
                    }

                    boolean isGivenNameMissing = StringUtils.isBlank(name.getGiven())
                            || "?".equals(StringUtils.trim(name.getGiven()));
                    String givenName = isGivenNameMissing ? NO_NAME : name.getGiven();
                    displayName = trimAndAppend(displayName, StringEscapeUtils.escapeHtml4(givenName));

                    if (StringUtils.isNotBlank(name.getNickname())) {
                        displayName = trimAndAppend(displayName, "<span class=\"fst-italic\">\"" + StringEscapeUtils.escapeHtml4(name.getNickname()) + "\"</span>");
                    }
                    if (StringUtils.isNotBlank(name.getSurnamePrefix())) {
                        displayName = trimAndAppend(displayName, "<span class=\"fw-semibold\">" + StringEscapeUtils.escapeHtml4(name.getSurnamePrefix()) + "</span>");
                    }
                    if (StringUtils.isNotBlank(name.getSurname())) {
                        displayName = trimAndAppend(displayName, "<span class=\"fw-semibold\">" + StringEscapeUtils.escapeHtml4(name.getSurname()) + "</span>");
                    }
                    if (StringUtils.isNotBlank(name.getSuffix())) {
                        displayName = trimAndAppend(displayName, StringEscapeUtils.escapeHtml4(name.getSuffix()) + "</span>", "<span class=\"text-secondary\">, ");
                    }

                    Optional<String> yob = PersonUtils.getDateOfBirth(person)
                            .flatMap(Date::parse)
                            .map(date -> ((date.getOperator() == Date.Operator.EST || date.getOperator() == Date.Operator.ABT)
                                    ? "aprox. " + date.getYear()
                                    : String.valueOf(date.getYear())));
                    boolean isAlive = PersonUtils.isAlive(person);
                    Optional<String> yod = PersonUtils.getDateOfDeath(person)
                            .flatMap(Date::parse)
                            .map(date -> ((date.getOperator() == Date.Operator.EST || date.getOperator() == Date.Operator.ABT)
                                    ? "aprox. " + date.getYear()
                                    : String.valueOf(date.getYear())));
                    if (yob.isPresent() || yod.isPresent()) {
                        String birthTag = yob.orElse("?");
                        String deathTag = isAlive ? "vive" : yod.orElse("?");
                        if (yob.isPresent()) {
                            Optional<String> pob = PersonUtils.getPlaceOfBirth(person);
                            if (pob.isPresent()) {
                                birthTag = "<span title=\"" + StringEscapeUtils.escapeHtml4(pob.get()) + "\">" + birthTag + "</span>";
                            }
                        }
                        if (yod.isPresent()) {
                            Optional<String> pod = PersonUtils.getPlaceOfDeath(person);
                            if (pod.isPresent()) {
                                deathTag = "<span title=\"" + StringEscapeUtils.escapeHtml4(pod.get()) + "\">" + deathTag + "</span>";
                            }
                        }
                        displayName = trimAndAppend(displayName, "<span class=\"small text-secondary ps-1\">(" + birthTag + "&ndash;" + deathTag + ")</span>");
                    }

                    return displayName;
                })
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(NO_NAME);
    }

    private static String trimAndAppend(String str, String append) {
        return trimAndAppend(str, append, " ");
    }

    private static String trimAndAppend(String str, String append, String separator) {
        append = append.trim();
        return StringUtils.isEmpty(str) ? append : str + separator + append;
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
                .map(simplified -> {
                    String normalized = NameUtils.normalizeGivenName(simplified, sex, normalizedGivenNamesMap);
                    return GivenName.of(givenName, simplified, normalized);
                });
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
                .map(simplified -> {
                    String normalizedMainWord = NameUtils.normalizeSurnameToMainWord(simplified, normalizedSurnamesMap);
                    String shortenedMainWord = NameUtils.shortenSurnameMainWord(normalizedMainWord);
                    return Surname.of(
                            surname,
                            simplified,
                            normalizedMainWord,
                            shortenedMainWord);
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

    /**
     * The main name's a.k.a. name (or former name).
     */
    public static Optional<Aka> getSimplifiedAka(
            @Nullable String aka,
            @Nullable SexType sex,
            Map<NameAndSex, String> normalizedGivenNamesMap) {
        return Optional.ofNullable(aka)
                .map(NameUtils::simplifyNameWithAlphabetConversion)
                .map(simplified -> {
                    Optional<GivenName> tentativeGivenName = Optional.of(simplified)
                            .map(s -> StringUtils.substringBeforeLast(s, " "))
                            .filter(str -> !str.isEmpty())
                            .flatMap(str -> PersonUtils.getNormalizedGivenName(str, sex, normalizedGivenNamesMap));
                    return Aka.of(aka, simplified, tentativeGivenName);
                });
    }

    /**
     * The main name's a.k.a. name (or former name).
     */
    public static Optional<Aka> getSimplifiedAka(
            Person person,
            Map<NameAndSex, String> normalizedGivenNamesMap) {
        Optional<String> aka = getAka(person);
        SexType sex = PersonUtils.getSex(person);
        return getSimplifiedAka(
                aka.orElse(null),
                sex,
                normalizedGivenNamesMap);
    }

    public static Optional<ProfilePicture> getProfilePicture(Person person) {
        return person.getMedia()
                .stream()
                .filter(media -> media.getExtensions()
                        .values()
                        .stream()
                        .map(gedcomTags -> (List<?>) gedcomTags)
                        .flatMap(List::stream)
                        .map(gedcomTag -> (GedcomTag) gedcomTag)
                        .anyMatch(gedcomTag -> gedcomTag.getTag().equals(PERSONAL_PHOTO_TAG)))
                .map(media -> new ProfilePicture(media.getFormat(), media.getFile()))
                .findFirst();
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

    public static List<Pair<Integer, Optional<ReferenceType>>> getParentsWithReference(Person legacyPerson, Gedcom legacyGedcom) {
        Map<Integer, Set<ReferenceType>> references = legacyPerson
                .getParentFamilyRefs()
                .stream()
                .sorted(Comparator.comparing(ParentFamilyRef::getRelationshipType, Comparator.nullsFirst(Comparator.naturalOrder())))
                .flatMap(familyRef -> {
                    Family family = familyRef.getFamily(legacyGedcom);
                    ReferenceType referenceType = PersonUtils.resolveParentReferenceType(familyRef.getRelationshipType());

                    return Stream
                            .of(
                                    family.getHusbands(legacyGedcom),
                                    family.getWives(legacyGedcom))
                            .flatMap(List::stream)
                            .map(PersonUtils::getId)
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
    public static List<Person> getAllSiblings(Person legacyPerson, Gedcom legacyGedcom) {
        return legacyPerson
                .getParentFamilies(legacyGedcom)
                .stream()
                .flatMap(family -> Stream
                        .of(
                                family.getHusbands(legacyGedcom),
                                family.getWives(legacyGedcom))
                        .flatMap(List::stream)
                        .map(parent -> parent.getSpouseFamilies(legacyGedcom))
                        .flatMap(List::stream)
                        .distinct())
                .map(family -> family.getChildren(legacyGedcom))
                .flatMap(List::stream)
                .filter(sibling -> !sibling.getId().equals(legacyPerson.getId()))
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

    public static List<SpouseWithChildren> getSpousesWithChildren(Person legacyPerson, Gedcom legacyGedcom, Map<String, Place> places) {
        return legacyPerson
                .getSpouseFamilies(legacyGedcom)
                .stream()
                .map(family -> {
                    List<Pair<Integer, Optional<ReferenceType>>> children = family
                            .getChildren(legacyGedcom)
                            .stream()
                            .map(child -> Pair.of(
                                    PersonUtils.getId(child),
                                    child
                                            .getParentFamilyRefs()
                                            .stream()
                                            .filter(parentFamilyRef -> parentFamilyRef.getRef().equals(family.getId()))
                                            .findFirst()
                                            .map(ParentFamilyRef::getRelationshipType)
                                            .map(PersonUtils::resolveChildReferenceType)))
                            .toList();
                    boolean isSeparated = FamilyUtils.isSeparated(family);
                    Optional<Date> dateOfPartners = FamilyUtils.getDateOfPartners(family)
                            .flatMap(Date::parse);
                    Optional<Date> dateOfSeparation = FamilyUtils.getDateOfSeparation(family)
                            .flatMap(Date::parse);
                    Optional<Place> placeOfPartners = FamilyUtils.getPlaceOfPartners(family)
                            .map(place -> Place.of(place, places));
                    Optional<Place> placeOfSeparation = FamilyUtils.getPlaceOfSeparation(family)
                            .map(place -> Place.of(place, places));
                    return Stream
                            .of(
                                    family.getHusbands(legacyGedcom),
                                    family.getWives(legacyGedcom))
                            .flatMap(List::stream)
                            .map(spouse -> spouse.getId().equals(legacyPerson.getId())
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
                        .orElseGet(() -> String.valueOf(RNG.nextInt()))))
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

    public static List<String> getEmails(Person person) {
        return person.getEventsFacts()
                .stream()
                .filter(eventFact -> RESI_TAGS.contains(eventFact.getTag()))
                .filter(eventFact -> EMAIL_TAG.equals(eventFact.getEmailTag()))
                .map(EventFact::getEmail)
                .toList();
    }

    public static Optional<ZonedDateTime> getUpdateDate(Person person, ZoneId zoneId) {
        return person.getExtensions()
                .values()
                .stream()
                .map(gedcomTags -> (List<?>) gedcomTags)
                .flatMap(List::stream)
                .map(gedcomTag -> (GedcomTag) gedcomTag)
                .filter(gedcomTag -> gedcomTag.getTag().equals(UPDATED_TAG))
                .map(GedcomTag::getValue)
                .map(StringUtils::trim)
                .map(updateDateStr -> {
                    Matcher matcher = UPDATE_DATE_PATTERN.matcher(updateDateStr);
                    if (matcher.matches()) {
                        // Date
                        return matcher.group(1) + " " + StringUtils.capitalize(matcher.group(2).toLowerCase()) + " " + matcher.group(3)
                                // Time
                                + " " + matcher.group(4)
                                // Zone
                                + " " + matcher.group(6);
                    }
                    return updateDateStr;
                })
                .map(updateDateStr -> OffsetDateTime.parse(updateDateStr, UPDATE_DATE_FORMATTER))
                .map(updateDate -> updateDate.atZoneSameInstant(zoneId))
                .findFirst();
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
            .<EnrichedPerson, Date>comparing(person -> person.getDateOfBirth().orElse(null), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(person -> person.getDateOfDeath().orElse(null), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(EnrichedPerson::getId);

    public static final Comparator<EnrichedPerson> AGES_COMPARATOR = Comparator
            .<EnrichedPerson, Age>comparing(person -> person.getAge().orElse(null), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(EnrichedPerson::getId);

    public static final Comparator<EnrichedPerson> NAME_COMPARATOR = Comparator
            .<EnrichedPerson, String>comparing(person -> person.getSurname().map(Surname::simplified).orElse(null), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(person -> person.getGivenName().map(GivenName::simplified).orElse(null), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(EnrichedPerson::getId);

}
