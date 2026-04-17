package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.service.SearchService;

import org.apache.commons.lang3.Strings;
import org.folg.gedcom.model.Gedcom;

import java.time.MonthDay;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.annotation.Nullable;

import lombok.Getter;

@Getter
public class EnrichedGedcom {

    private static final Set<Integer> AZUL_MAYOR_EXCLUDED_IDS = Set.of(504379, 545429, 552956);
    private static final String[] AZUL_MAYOR_TITLE_PREFIXES = {"Jz. Pz.", "Int. Mun.", "Pte. Mun.", "Com. Mun.", "Del. Mun."};

    private final Gedcom legacyGedcom;
    private final String gedcomName;
    @Nullable
    private final ZonedDateTime modifiedDateTime;
    private final GedcomAnalyzerProperties properties;

    private final List<EnrichedPerson> people;

    // General stats
    private final Integer familiesCount;
    private final Integer maleCount;
    private final Integer femaleCount;
    private final Integer aliveCount;
    private final Integer deceasedCount;
    private final Integer distinguishedCount;
    private final Integer nativeCount;

    // Azul specific stats
    private final Integer azulPersonsCount;
    private final Integer azulAliveCount;
    private final Integer azulMayorsCount;
    private final Integer azulDisappearedCount;

    // Indexes
    private final Map<Integer, EnrichedPerson> peopleByIdIndex;
    private final Map<UUID, EnrichedPerson> peopleByUuidIndex;
    private final Map<NameAndSex, List<EnrichedPerson>> peopleByNormalizedSurnameMainWordAndSexIndex;
    private final Map<NameSexYear, List<EnrichedPerson>> peopleByNormalizedSurnameMainWordAndSexAndYearOfBirthIndex;
    private final Map<NameSexYear, List<EnrichedPerson>> peopleByNormalizedSurnameMainWordAndSexAndYearOfDeathIndex;
    private final Map<MonthDay, List<EnrichedPerson>> azulAlivePersonsByBirthdayIndex;
    private final Map<MonthDay, List<EnrichedPerson>> distinguishedPersonsByBirthdayIndex;
    private final Map<MonthDay, List<EnrichedPerson>> distinguishedPersonsByDeathdayIndex;

    private final Map<String, Place> places = new HashMap<>(256);

    private EnrichedGedcom(
            Gedcom legacyGedcom,
            String gedcomName,
            @Nullable ZonedDateTime modifiedDateTime,
            GedcomAnalyzerProperties properties,
            SearchService searchService) {

        this.legacyGedcom = properties.isKeepReferenceToLegacyGedcom() ? legacyGedcom : null;
        this.gedcomName = gedcomName;
        this.modifiedDateTime = modifiedDateTime;
        this.properties = properties;

        this.people = getEnrichedPeople(legacyGedcom);

        // General stats + all lookup indexes — single pass over the full people list
        this.familiesCount = legacyGedcom.getFamilies().size();
        int maleCount = 0, aliveCount = 0, distinguishedCount = 0, nativeCount = 0;
        int azulMayorsCount = 0, azulDisappearedCount = 0;
        Map<Integer, EnrichedPerson> byId = new HashMap<>(this.people.size() * 2);
        Map<UUID, EnrichedPerson> byUuid = new HashMap<>(this.people.size() * 2);
        Map<NameAndSex, List<EnrichedPerson>> bySurnameAndSex = new HashMap<>();
        Map<NameSexYear, List<EnrichedPerson>> bySurnameAndSexAndBirthYear = new HashMap<>();
        Map<NameSexYear, List<EnrichedPerson>> bySurnameAndSexAndDeathYear = new HashMap<>();
        Map<MonthDay, List<EnrichedPerson>> distinguishedByBirthday = new HashMap<>();
        Map<MonthDay, List<EnrichedPerson>> distinguishedByDeathday = new HashMap<>();

        for (EnrichedPerson person : this.people) {
            if (person.getSex() == SexType.M) maleCount++;
            if (person.isAlive()) aliveCount++;
            if (person.isDistinguishedPerson()) {
                distinguishedCount++;
                if (!AZUL_MAYOR_EXCLUDED_IDS.contains(person.getId())
                        && Strings.CS.startsWithAny(person.getDisplayName(), AZUL_MAYOR_TITLE_PREFIXES)) {
                    azulMayorsCount++;
                }
                // Index living personalities by birth day for the efemérides section
                if (person.isAlive()) {
                    person.getDateOfBirth()
                            .filter(Date::isFullDate)
                            .ifPresent(dob -> distinguishedByBirthday
                                    .computeIfAbsent(MonthDay.of(dob.getMonth(), dob.getDay()), _ -> new ArrayList<>())
                                    .add(person));
                } else {
                    // Index deceased personalities by death day for the efemérides section
                    person.getDateOfDeath()
                            .filter(Date::isFullDate)
                            .ifPresent(dod -> distinguishedByDeathday
                                    .computeIfAbsent(MonthDay.of(dod.getMonth(), dod.getDay()), _ -> new ArrayList<>())
                                    .add(person));
                }
            }
            if (person.isNativePerson()) nativeCount++;
            if (person.isDisappearedPerson()) azulDisappearedCount++;

            byId.put(person.getId(), person);
            byUuid.put(person.getUuid(), person);

            String surnameKey = person.getSurname().map(Surname::shortenedMainWord).orElse(null);
            if (surnameKey != null && person.getSex() != SexType.U) {
                bySurnameAndSex
                        .computeIfAbsent(new NameAndSex(surnameKey, person.getSex()), _ -> new ArrayList<>())
                        .add(person);
                populateNameSexYearIndex(bySurnameAndSexAndBirthYear, person, surnameKey, person.getDateOfBirth().orElse(null));
                populateNameSexYearIndex(bySurnameAndSexAndDeathYear, person, surnameKey, person.getDateOfDeath().orElse(null));
            }
        }

        this.maleCount = maleCount;
        this.femaleCount = this.people.size() - maleCount;
        this.aliveCount = aliveCount;
        this.deceasedCount = this.people.size() - aliveCount;
        this.distinguishedCount = distinguishedCount;
        this.nativeCount = nativeCount;
        this.azulMayorsCount = azulMayorsCount;
        // TODO: azulDisappearedCount counts disappeared persons across all places, not just those
        //       linked to Azul. Consider filtering by Azul place in a future improvement.
        this.azulDisappearedCount = azulDisappearedCount;
        this.peopleByIdIndex = byId;
        this.peopleByUuidIndex = byUuid;
        this.peopleByNormalizedSurnameMainWordAndSexIndex = bySurnameAndSex;
        this.peopleByNormalizedSurnameMainWordAndSexAndYearOfBirthIndex = bySurnameAndSexAndBirthYear;
        this.peopleByNormalizedSurnameMainWordAndSexAndYearOfDeathIndex = bySurnameAndSexAndDeathYear;
        this.distinguishedPersonsByBirthdayIndex = distinguishedByBirthday;
        this.distinguishedPersonsByDeathdayIndex = distinguishedByDeathday;

        // Azul place-linked stats (separate pass required — involves place traversal)
        List<EnrichedPerson> azulAllPersons = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", null, null, true, false, false, this.people);
        this.azulPersonsCount = azulAllPersons.size();
        List<EnrichedPerson> azulAlivePersons = azulAllPersons.stream().filter(EnrichedPerson::isAlive).toList();
        this.azulAliveCount = azulAlivePersons.size();

        this.azulAlivePersonsByBirthdayIndex = azulAlivePersons
                .stream()
                .filter(person -> person.getDateOfBirth().filter(Date::isFullDate).isPresent())
                .collect(Collectors.groupingBy(person -> {
                    Date dob = person.getDateOfBirth().get();
                    return MonthDay.of(dob.getMonth(), dob.getDay());
                }));
    }

    public static EnrichedGedcom of(
            Gedcom legacyGedcom,
            String gedcomName,
            ZonedDateTime modifiedDateTime,
            GedcomAnalyzerProperties properties,
            SearchService searchService) {
        return new EnrichedGedcom(legacyGedcom, gedcomName, modifiedDateTime, properties, searchService);
    }

    public static EnrichedGedcom of(
            Gedcom legacyGedcom,
            String gedcomName,
            GedcomAnalyzerProperties properties,
            SearchService searchService) {
        return new EnrichedGedcom(legacyGedcom, gedcomName, null, properties, searchService);
    }

    public Optional<Gedcom> getLegacyGedcom() {
        return Optional.ofNullable(legacyGedcom);
    }

    private List<EnrichedPerson> getEnrichedPeople(Gedcom legacyGedcom) {
        List<EnrichedPerson> enrichedPeople = legacyGedcom.getPeople()
                .stream()
                .map(p -> EnrichedPerson.of(p, this))
                .toList();

        Map<Integer, EnrichedPerson> enrichedPeopleIndex = enrichedPeople
                .stream()
                .collect(Collectors.toUnmodifiableMap(EnrichedPerson::getId, p -> p));

        enrichedPeople
                .forEach(person -> person.enrichFamily(legacyGedcom, enrichedPeopleIndex));

        return enrichedPeople;
    }

    @Nullable
    public EnrichedPerson getPersonById(Integer id) {
        return peopleByIdIndex.get(id);
    }

    @Nullable
    public EnrichedPerson getPersonByUuid(UUID uuid) {
        return peopleByUuidIndex.get(uuid);
    }

    public List<EnrichedPerson> getPersonsBySurnameMainWordAndSex(Surname surname, SexType sex) {
        NameAndSex nameAndSex = new NameAndSex(surname.shortenedMainWord(), sex);
        List<EnrichedPerson> persons = peopleByNormalizedSurnameMainWordAndSexIndex.getOrDefault(nameAndSex, List.of());
        return getPersonsMatchingSurname(surname, persons);
    }

    public List<EnrichedPerson> getPersonsBySurnameMainWordAndSexAndYearOfBirthIndex(Surname surname, SexType sex, Year yearOfBirth) {
        NameSexYear nameSexYear = new NameSexYear(surname.shortenedMainWord(), sex, yearOfBirth);
        List<EnrichedPerson> persons = peopleByNormalizedSurnameMainWordAndSexAndYearOfBirthIndex.getOrDefault(nameSexYear, List.of());
        return getPersonsMatchingSurname(surname, persons);
    }

    public List<EnrichedPerson> getPersonsBySurnameMainWordAndSexAndYearOfDeathIndex(Surname surname, SexType sex, Year yearOfDeath) {
        NameSexYear nameSexYear = new NameSexYear(surname.shortenedMainWord(), sex, yearOfDeath);
        List<EnrichedPerson> persons = peopleByNormalizedSurnameMainWordAndSexAndYearOfDeathIndex.getOrDefault(nameSexYear, List.of());
        return getPersonsMatchingSurname(surname, persons);
    }

    private void populateNameSexYearIndex(
            Map<NameSexYear, List<EnrichedPerson>> index,
            EnrichedPerson person,
            String surnameKey,
            @Nullable Date date) {
        if (date == null || date.getOperator() == Date.Operator.BEF || date.getOperator() == Date.Operator.AFT) {
            return;
        }
        List<Year> years;
        // Secondary date represents a from-to range: include every year in between
        if (date.getSecondary() != null) {
            years = IntStream
                    .rangeClosed(date.getYear().getValue(), date.getSecondary().getYear().getValue())
                    .mapToObj(Year::of)
                    .toList();
        // Estimated only-year date: include the year before and after to widen the search window
        } else if (date.isOnlyYearDate()
                && (date.getOperator() == Date.Operator.ABT || date.getOperator() == Date.Operator.EST)) {
            years = List.of(date.getYear().minusYears(1), date.getYear(), date.getYear().plusYears(1));
        } else {
            years = List.of(date.getYear());
        }
        for (Year year : years) {
            index.computeIfAbsent(new NameSexYear(surnameKey, person.getSex(), year), _ -> new ArrayList<>())
                    .add(person);
        }
    }

    private List<EnrichedPerson> getPersonsMatchingSurname(Surname surname, List<EnrichedPerson> persons) {
        return persons
                .stream()
                .filter(person -> surname.matches(person.getSurname().orElse(null)))
                .toList();
    }

    public void analyzeCustomEventFactsAndTagExtensions() {
        getLegacyGedcom()
                .ifPresent(gedcom -> people.forEach(person -> person.analyzeCustomEventFactsAndTagExtensions(gedcom)));
    }

}
