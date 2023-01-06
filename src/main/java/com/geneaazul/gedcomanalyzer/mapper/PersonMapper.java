package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResult;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateCompareDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateDto;
import com.geneaazul.gedcomanalyzer.model.dto.SpouseWithChildrenDto;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class PersonMapper {

    private static final String PRIVATE_NAME = "<private>";
    private static final String PRIVATE_DATE = "<private>";
    private static final String NO_SPOUSE = "<no spouse>";

    public List<PersonDto> toPersonDto(Collection<EnrichedPerson> persons) {
        return persons
                .stream()
                .map(this::toPersonDto)
                .toList();
    }

    public PersonDto toPersonDto(EnrichedPerson person) {
        return toPersonDto(person, ObfuscationType.NONE, ep -> List.of());
    }

    public List<PersonDto> toPersonDto(
            Collection<EnrichedPerson> persons,
            ObfuscationType obfuscationType,
            Function<EnrichedPerson, List<String>> ancestryCountriesResolver) {
        return persons
                .stream()
                .map(person -> toPersonDto(person, obfuscationType, ancestryCountriesResolver))
                .toList();
    }

    public PersonDto toPersonDto(
            EnrichedPerson person,
            ObfuscationType obfuscationType,
            Function<EnrichedPerson, List<String>> ancestryCountriesResolver) {

        boolean obfuscateLiving = obfuscationType != ObfuscationType.NONE;
        boolean obfuscateName = obfuscateLiving && obfuscationType != ObfuscationType.SKIP_MAIN_PERSON_NAME;

        List<SpouseWithChildrenDto> spouses = toSpouseWithChildrenDto(
                person.getSpousesWithChildren(),
                obfuscateLiving,
                person.isAlive());

        return PersonDto.builder()
                .sex(person.getSex())
                .isAlive(person.isAlive())
                .name(obfuscateName(
                        person,
                        obfuscateName && person.isAlive()))
                .dateOfBirth(person.getDateOfBirth()
                        .map(date -> obfuscateLiving && person.isAlive()
                                ? PRIVATE_DATE
                                : date.format())
                        .orElse(null))
                .placeOfBirth(person.getCountryOfBirthForSearch()
                        .orElse(null))
                .dateOfDeath(person.getDateOfDeath()
                        .map(Date::format)
                        .orElse(null))
                .parents(person.getParents()
                        .stream()
                        .map(parent -> obfuscateName(
                                parent,
                                obfuscateLiving && (person.isAlive() || parent.isAlive())))
                        .toList())
                .spouses(spouses)
                .ancestryCountries(ancestryCountriesResolver.apply(person))
                .build();
    }

    public List<SpouseWithChildrenDto> toSpouseWithChildrenDto(
            Collection<Pair<Optional<EnrichedPerson>, List<EnrichedPerson>>> spouseChildrenPairs,
            boolean obfuscateLiving,
            boolean mainPersonIsAlive) {
        return spouseChildrenPairs
                .stream()
                .map(pair -> toSpouseWithChildrenDto(pair, obfuscateLiving, mainPersonIsAlive))
                .toList();
    }

    public SpouseWithChildrenDto toSpouseWithChildrenDto(
            Pair<Optional<EnrichedPerson>, List<EnrichedPerson>> spouseChildrenPair,
            boolean obfuscateLiving,
            boolean mainPersonIsAlive) {

        boolean spouseIsAlive = spouseChildrenPair.getLeft()
                .map(EnrichedPerson::isAlive)
                .orElse(false);

        return SpouseWithChildrenDto.builder()
                .name(obfuscateSpouseName(
                        spouseChildrenPair.getLeft(),
                        spouse -> obfuscateLiving && (mainPersonIsAlive || spouse.isAlive())))
                .children(spouseChildrenPair.getRight()
                        .stream()
                        .map(child -> obfuscateName(
                                child,
                                obfuscateLiving && (mainPersonIsAlive || spouseIsAlive || child.isAlive())))
                        .toList())
                .build();
    }

    private static String obfuscateName(
            EnrichedPerson person,
            boolean condition) {
        return condition
                ? person.getSurname()
                        .map(surname -> PRIVATE_NAME + " " + surname)
                        .orElse(PRIVATE_NAME)
                : person.getDisplayName();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static String obfuscateSpouseName(
            Optional<EnrichedPerson> maybeSpouse,
            Predicate<EnrichedPerson> condition) {
        return maybeSpouse
                .map(spouse -> obfuscateName(spouse, condition.test(spouse)))
                .orElse(NO_SPOUSE);
    }

    public List<PersonDuplicateDto> toPersonDuplicateDto(
            Collection<PersonComparisonResults> personComparisonResults) {
        return personComparisonResults
                .stream()
                .map(this::toPersonDuplicateDto)
                .toList();
    }

    public PersonDuplicateDto toPersonDuplicateDto(
            PersonComparisonResults personComparisonResults) {
        return PersonDuplicateDto.builder()
                .person(toPersonDto(personComparisonResults.getPerson()))
                .duplicates(toPersonDuplicateCompareDto(personComparisonResults.getResults()))
                .build();
    }

    private List<PersonDuplicateCompareDto> toPersonDuplicateCompareDto(
            Collection<PersonComparisonResult> personComparisonResults) {
        return personComparisonResults
                .stream()
                .map(this::toPersonDuplicateDto)
                .toList();
    }

    private PersonDuplicateCompareDto toPersonDuplicateDto(
            PersonComparisonResult personComparisonResult) {
        return PersonDuplicateCompareDto.builder()
                .person(toPersonDto(personComparisonResult.getCompare()))
                .score(personComparisonResult.getScore())
                .build();
    }

}
