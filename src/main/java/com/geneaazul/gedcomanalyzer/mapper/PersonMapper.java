package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResult;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateCompareDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateDto;
import com.geneaazul.gedcomanalyzer.model.dto.SpouseWithChildrenDto;
import com.geneaazul.gedcomanalyzer.utils.DateUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
public class PersonMapper {

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
                .name(PersonUtils.obfuscateName(person, obfuscateName && person.isAlive()))
                .dateOfBirth(person.getDateOfBirth()
                        .map(date -> DateUtils.obfuscateDate(date, obfuscateLiving && person.isAlive()))
                        .orElse(null))
                .placeOfBirth(person.getCountryOfBirthForSearch()
                        .orElse(null))
                .dateOfDeath(person.getDateOfDeath()
                        .map(Date::format)
                        .orElse(null))
                .parents(person.getParents()
                        .stream()
                        .map(parent -> PersonUtils.obfuscateName(
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
                .name(PersonUtils.obfuscateSpouseName(
                        spouseChildrenPair.getLeft(),
                        spouse -> obfuscateLiving && (mainPersonIsAlive || spouse.isAlive())))
                .children(spouseChildrenPair.getRight()
                        .stream()
                        .map(child -> PersonUtils.obfuscateName(
                                child,
                                obfuscateLiving && (mainPersonIsAlive || spouseIsAlive || child.isAlive())))
                        .toList())
                .build();
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
