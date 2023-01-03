package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.Gedcom;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;

@Getter
public class EnrichedGedcom {

    private final Gedcom gedcom;
    private final String gedcomName;
    private final GedcomAnalyzerProperties properties;

    private final List<EnrichedPerson> people;

    // Indexes
    private final Map<String, EnrichedPerson> peopleByIdIndex;
    private final Map<NameAndSex, List<EnrichedPerson>> peopleBySurnameMainWordForSearchAndSexIndex;
    private final Map<NameSexYear, List<EnrichedPerson>> peopleBySurnameMainWordForSearchAndSexAndYearOfBirthIndex;
    private final Map<NameSexYear, List<EnrichedPerson>> peopleBySurnameMainWordForSearchAndSexAndYearOfDeathIndex;

    private EnrichedGedcom(Gedcom gedcom, String gedcomName, GedcomAnalyzerProperties properties) {
        this.gedcom = gedcom;
        this.gedcomName = gedcomName;
        this.properties = properties;

        this.people = getEnrichedPeople();

        this.peopleByIdIndex = this.people
                .stream()
                .collect(Collectors.toMap(EnrichedPerson::getId, Function.identity()));

        this.peopleBySurnameMainWordForSearchAndSexIndex = this.people
                .stream()
                .filter(person -> person.getSurnameMainWordForSearch().isPresent())
                .filter(person -> person.getSex() != SexType.U)
                .collect(Collectors.groupingBy(person -> new NameAndSex(person.getSurnameMainWordForSearch().get(), person.getSex())));

        this.peopleBySurnameMainWordForSearchAndSexAndYearOfBirthIndex = buildNameSexYearIndex(
                person -> person.getSurnameMainWordForSearch().orElse(null),
                EnrichedPerson::getSex,
                person -> person.getDateOfBirth().orElse(null));

        this.peopleBySurnameMainWordForSearchAndSexAndYearOfDeathIndex = buildNameSexYearIndex(
                person -> person.getSurnameMainWordForSearch().orElse(null),
                EnrichedPerson::getSex,
                person -> person.getDateOfDeath().orElse(null));
    }

    public static EnrichedGedcom of(Gedcom gedcom, String gedcomName, GedcomAnalyzerProperties properties) {
        return new EnrichedGedcom(gedcom, gedcomName, properties);
    }

    private List<EnrichedPerson> getEnrichedPeople() {
        List<EnrichedPerson> enrichedPeople = gedcom.getPeople()
                .stream()
                .map(p -> EnrichedPerson.of(p, gedcom, properties))
                .toList();

        Map<String, EnrichedPerson> enrichedPeopleIndex = enrichedPeople
                .stream()
                .collect(Collectors.toUnmodifiableMap(EnrichedPerson::getId, Function.identity()));

        enrichedPeople
                .forEach(person -> person.enrichFamily(enrichedPeopleIndex));

        return enrichedPeople;
    }

    public EnrichedPerson getPersonById(String id) {
        return peopleByIdIndex.get(id);
    }

    public List<EnrichedPerson> getPersonsBySurnameMainWordAndSex(String surnameMainWord, SexType sex) {
        return peopleBySurnameMainWordForSearchAndSexIndex.getOrDefault(new NameAndSex(surnameMainWord, sex), List.of());
    }

    public List<EnrichedPerson> getPersonsBySurnameMainWordAndSexAndYearOfBirthIndex(String surnameMainWord, SexType sex, Year yearOfBirth) {
        return peopleBySurnameMainWordForSearchAndSexAndYearOfBirthIndex.getOrDefault(new NameSexYear(surnameMainWord, sex, yearOfBirth), List.of());
    }

    public List<EnrichedPerson> getPersonsBySurnameMainWordAndSexAndYearOfDeathIndex(String surnameMainWord, SexType sex, Year yearOfDeath) {
        return peopleBySurnameMainWordForSearchAndSexAndYearOfDeathIndex.getOrDefault(new NameSexYear(surnameMainWord, sex, yearOfDeath), List.of());
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

}
