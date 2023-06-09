package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.Gedcom;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;

import lombok.Getter;

@Getter
public class EnrichedGedcom {

    private final Gedcom gedcom;
    private final String gedcomName;
    private final GedcomAnalyzerProperties properties;

    private final List<EnrichedPerson> people;

    // Indexes
    private final Map<String, EnrichedPerson> peopleByIdIndex;
    private final Map<UUID, EnrichedPerson> peopleByUuidIndex;
    private final Map<NameAndSex, List<EnrichedPerson>> peopleByNormalizedSurnameMainWordAndSexIndex;
    private final Map<NameSexYear, List<EnrichedPerson>> peopleByNormalizedSurnameMainWordAndSexAndYearOfBirthIndex;
    private final Map<NameSexYear, List<EnrichedPerson>> peopleByNormalizedSurnameMainWordAndSexAndYearOfDeathIndex;

    private EnrichedGedcom(Gedcom gedcom, String gedcomName, GedcomAnalyzerProperties properties) {
        this.gedcom = gedcom;
        this.gedcomName = gedcomName;
        this.properties = properties;

        this.people = getEnrichedPeople();

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

    public static EnrichedGedcom of(Gedcom gedcom, String gedcomName, GedcomAnalyzerProperties properties) {
        return new EnrichedGedcom(gedcom, gedcomName, properties);
    }

    private List<EnrichedPerson> getEnrichedPeople() {
        List<EnrichedPerson> enrichedPeople = gedcom.getPeople()
                .stream()
                .map(p -> EnrichedPerson.of(p, this))
                .toList();

        Map<String, EnrichedPerson> enrichedPeopleIndex = enrichedPeople
                .stream()
                .collect(Collectors.toUnmodifiableMap(EnrichedPerson::getId, Function.identity()));

        enrichedPeople
                .forEach(person -> person.enrichFamily(enrichedPeopleIndex));

        return enrichedPeople;
    }

    @CheckForNull
    public EnrichedPerson getPersonById(String id) {
        return peopleByIdIndex.get(id);
    }

    @CheckForNull
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
        people.forEach(EnrichedPerson::analyzeCustomEventFactsAndTagExtensions);
    }

}
