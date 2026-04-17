package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.service.SearchService;

import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.Gedcom;

import java.time.MonthDay;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.annotation.Nullable;

import lombok.Getter;

@Getter
public class EnrichedGedcom {

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

        // General stats
        this.familiesCount = legacyGedcom.getFamilies().size();
        this.maleCount = Math.toIntExact(this.people.stream().filter(person -> person.getSex() == SexType.M).count());
        this.femaleCount = this.people.size() - this.maleCount;
        this.aliveCount = Math.toIntExact(this.people.stream().filter(EnrichedPerson::isAlive).count());
        this.deceasedCount = this.people.size() - this.aliveCount;
        this.distinguishedCount = Math.toIntExact(this.people.stream().filter(EnrichedPerson::isDistinguishedPerson).count());
        this.nativeCount = Math.toIntExact(this.people.stream().filter(EnrichedPerson::isNativePerson).count());

        // Azul specific stats
        List<EnrichedPerson> azulAllPersons = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", null, null, true, false, false, this.people);
        this.azulPersonsCount = azulAllPersons.size();
        List<EnrichedPerson> azulAlivePersons = azulAllPersons.stream().filter(EnrichedPerson::isAlive).toList();
        this.azulAliveCount = azulAlivePersons.size();
        this.azulMayorsCount = Math.toIntExact(this.people
                .stream()
                .filter(EnrichedPerson::isDistinguishedPerson)
                .filter(person -> !Set.of(504379, 545429, 552956).contains(person.getId()))
                .filter(person -> Strings.CS.startsWithAny(person.getDisplayName(), "Jz. Pz.", "Int. Mun.", "Pte. Mun.", "Com. Mun.", "Del. Mun."))
                .count());
        // TODO: azulDisappearedCount counts disappeared persons across all places, not just those
        //       linked to Azul. Consider filtering by Azul place in a future improvement.
        this.azulDisappearedCount = Math.toIntExact(this.people.stream().filter(EnrichedPerson::isDisappearedPerson).count());

        this.azulAlivePersonsByBirthdayIndex = azulAlivePersons
                .stream()
                .filter(person -> person.getDateOfBirth().filter(Date::isFullDate).isPresent())
                .collect(Collectors.groupingBy(person -> {
                    Date dob = person.getDateOfBirth().get();
                    return MonthDay.of(dob.getMonth(), dob.getDay());
                }));

        this.peopleByIdIndex = this.people
                .stream()
                .collect(Collectors.toMap(EnrichedPerson::getId, Function.identity()));

        this.peopleByUuidIndex = this.people
                .stream()
                .collect(Collectors.toMap(EnrichedPerson::getUuid, Function.identity()));

        this.peopleByNormalizedSurnameMainWordAndSexIndex = this.people
                .stream()
                .filter(person -> person.getSurname().isPresent())
                .filter(person -> person.getSex() != SexType.U)
                .collect(Collectors.groupingBy(person -> new NameAndSex(person.getSurname().get().shortenedMainWord(), person.getSex())));

        this.peopleByNormalizedSurnameMainWordAndSexAndYearOfBirthIndex = buildNameSexYearIndex(
                person -> person.getSurname()
                        .map(Surname::shortenedMainWord)
                        .orElse(null),
                EnrichedPerson::getSex,
                person -> person.getDateOfBirth().orElse(null));

        this.peopleByNormalizedSurnameMainWordAndSexAndYearOfDeathIndex = buildNameSexYearIndex(
                person -> person.getSurname()
                        .map(Surname::shortenedMainWord)
                        .orElse(null),
                EnrichedPerson::getSex,
                person -> person.getDateOfDeath().orElse(null));
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
                .collect(Collectors.toUnmodifiableMap(EnrichedPerson::getId, Function.identity()));

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

    private Map<NameSexYear, List<EnrichedPerson>> buildNameSexYearIndex(
            Function<EnrichedPerson, String> nameMapper,
            Function<EnrichedPerson, SexType> sexMapper,
            Function<EnrichedPerson, Date> dateMapper) {
        return this.people
                .stream()
                .filter(person -> nameMapper.apply(person) != null)
                .filter(person -> sexMapper.apply(person) != SexType.U)
                .filter(person -> {
                    Date date = dateMapper.apply(person);
                    return date != null && date.getOperator() != Date.Operator.BEF && date.getOperator() != Date.Operator.AFT;
                })
                .map(person -> {
                    Date date = dateMapper.apply(person);
                    // Secondary date is used to represent from-to dates, when it is set we don't check whether the dates are estimated or not
                    if (date.getSecondary() != null) {
                        return IntStream
                                .rangeClosed(
                                        date.getYear().getValue(),
                                        date.getSecondary().getYear().getValue())
                                .mapToObj(year -> Pair.of(person, Year.of(year)))
                                .toList();
                    }
                    // When it is an estimated only-year date we consider a year before and a year after
                    if (date.isOnlyYearDate()
                            && (date.getOperator() == Date.Operator.ABT || date.getOperator() == Date.Operator.EST)) {
                        return List.of(
                                Pair.of(person, date.getYear().minusYears(1)),
                                Pair.of(person, date.getYear()),
                                Pair.of(person, date.getYear().plusYears(1)));
                    }
                    return List.of(Pair.of(person, date.getYear()));
                })
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        pair -> new NameSexYear(nameMapper.apply(pair.getLeft()), sexMapper.apply(pair.getLeft()), pair.getRight()),
                        Collectors.mapping(Pair::getLeft, Collectors.toList())));
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
