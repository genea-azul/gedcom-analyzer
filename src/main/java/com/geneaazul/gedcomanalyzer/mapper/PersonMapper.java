package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResult;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateCompareDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateDto;
import com.geneaazul.gedcomanalyzer.model.dto.SpouseWithChildrenDto;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.Person;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class PersonMapper {

    public List<PersonDto> toPersonDto(
            Collection<EnrichedPerson> persons,
            ObfuscationType obfuscationType) {
        return persons
                .stream()
                .map(person -> toPersonDto(person, obfuscationType))
                .toList();
    }

    public PersonDto toPersonDto(
            EnrichedPerson person,
            ObfuscationType obfuscationType) {

        boolean obfuscateLiving = obfuscationType != ObfuscationType.NONE;
        boolean obfuscateName = obfuscateLiving && obfuscationType != ObfuscationType.SKIP_MAIN_PERSON_NAME;

        List<SpouseWithChildrenDto> spouses = toSpouseWithChildrenDto(
                PersonUtils.getSpousesWithChildren(person.getPerson(), person.getGedcom()),
                obfuscateLiving,
                person.isAlive());

        return PersonDto.builder()
                .sex(person.getSex())
                .isAlive(person.isAlive())
                .name(obfuscateName && person.isAlive()
                        ? person.getSurname()
                                .map(surname -> "<private> " + surname)
                                .orElse("<private>")
                        : person.getDisplayName())
                .dateOfBirth(person.getDateOfBirth()
                        .map(date -> obfuscateLiving && person.isAlive()
                                ? "<private>"
                                : date.format())
                        .orElse(null))
                .placeOfBirth(person.getCountryOfBirthForSearch()
                        .orElse(null))
                .dateOfDeath(person.getDateOfDeath()
                        .map(Date::format)
                        .orElse(null))
                .parents(person.getParents()
                        .stream()
                        .map(parent -> obfuscateLiving && (person.isAlive() || parent.isAlive())
                                ? parent.getSurname()
                                        .map(surname -> "<private> " + surname)
                                        .orElse("<private>")
                                : parent.getDisplayName())
                        .toList())
                .spouses(spouses)
                .build();
    }

    public List<SpouseWithChildrenDto> toSpouseWithChildrenDto(
            Collection<Pair<Optional<Person>, List<Person>>> spouseChildrenPairs,
            boolean obfuscateLiving,
            boolean mainPersonIsAlive) {
        return spouseChildrenPairs
                .stream()
                .map(pair -> toSpouseWithChildrenDto(pair, obfuscateLiving, mainPersonIsAlive))
                .toList();
    }

    public SpouseWithChildrenDto toSpouseWithChildrenDto(
            Pair<Optional<Person>, List<Person>> spouseChildrenPair,
            boolean obfuscateLiving,
            boolean mainPersonIsAlive) {

        boolean spouseIsAlive = spouseChildrenPair.getLeft()
                .map(PersonUtils::isAlive)
                .orElse(false);

        return SpouseWithChildrenDto.builder()
                .name(spouseChildrenPair.getLeft()
                        .map(spouse -> obfuscateLiving && (mainPersonIsAlive || spouseIsAlive)
                                ? PersonUtils.getSurname(spouse)
                                        .map(surname -> "<private> " + surname)
                                        .orElse("<private>")
                                : PersonUtils.getDisplayName(spouse))
                        .orElse("<no spouse>"))
                .children(spouseChildrenPair.getRight()
                        .stream()
                        .map(child -> obfuscateLiving && (mainPersonIsAlive || spouseIsAlive || PersonUtils.isAlive(child))
                                ? PersonUtils.getSurname(child)
                                        .map(surname -> "<private> " + surname)
                                        .orElse("<private>")
                                : PersonUtils.getDisplayName(child))
                        .toList())
                .build();
    }

    public List<PersonDuplicateDto> toPersonDuplicateDto(
            Collection<PersonComparisonResults> personComparisonResults,
            ObfuscationType obfuscationType) {
        return personComparisonResults
                .stream()
                .map(results -> toPersonDuplicateDto(results, obfuscationType))
                .toList();
    }

    public PersonDuplicateDto toPersonDuplicateDto(
            PersonComparisonResults personComparisonResults,
            ObfuscationType obfuscationType) {
        return PersonDuplicateDto.builder()
                .person(toPersonDto(personComparisonResults.getPerson(), obfuscationType))
                .duplicates(toPersonDuplicateCompareDto(personComparisonResults.getResults(), obfuscationType))
                .build();
    }

    private List<PersonDuplicateCompareDto> toPersonDuplicateCompareDto(
            Collection<PersonComparisonResult> personComparisonResults,
            ObfuscationType obfuscationType) {
        return personComparisonResults
                .stream()
                .map(result -> toPersonDuplicateDto(result, obfuscationType))
                .toList();
    }

    private PersonDuplicateCompareDto toPersonDuplicateDto(
            PersonComparisonResult personComparisonResult,
            ObfuscationType obfuscationType) {
        return PersonDuplicateCompareDto.builder()
                .person(toPersonDto(personComparisonResult.getCompare(), obfuscationType))
                .score(personComparisonResult.getScore())
                .build();
    }

}
