package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.domain.Date;
import com.geneaazul.gedcomanalyzer.domain.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.domain.PersonComparisonResult;
import com.geneaazul.gedcomanalyzer.domain.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.PersonDto;
import com.geneaazul.gedcomanalyzer.model.PersonDuplicateCompareDto;
import com.geneaazul.gedcomanalyzer.model.PersonDuplicateDto;
import com.geneaazul.gedcomanalyzer.model.SpouseWithChildrenDto;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.Person;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class PersonMapper {

    public List<PersonDto> toPersonDto(Collection<EnrichedPerson> persons) {
        return persons
                .stream()
                .map(this::toPersonDto)
                .toList();
    }

    public PersonDto toPersonDto(EnrichedPerson person) {

        List<SpouseWithChildrenDto> spouses = toSpouseWithChildrenDto(PersonUtils.getSpousesWithChildren(person.getPerson(), person.getGedcom()));

        return PersonDto.builder()
                .id(person.getId())
                .sex(person.getSex())
                .isAlive(person.isAlive())
                .name(person.getDisplayName())
                .dateOfBirth(person.getDateOfBirth()
                        .map(Date::format)
                        .orElse(null))
                .dateOfDeath(person.getDateOfDeath()
                        .map(Date::format)
                        .orElse(null))
                .parents(person.getParents()
                        .stream()
                        .map(EnrichedPerson::getDisplayName)
                        .toList())
                .spouses(spouses)
                .build();
    }

    public List<SpouseWithChildrenDto> toSpouseWithChildrenDto(Collection<Pair<Optional<Person>, List<String>>> spouseChildrenPairs) {
        return spouseChildrenPairs
                .stream()
                .map(this::toSpouseWithChildrenDto)
                .toList();
    }

    public SpouseWithChildrenDto toSpouseWithChildrenDto(Pair<Optional<Person>, List<String>> spouseChildrenPair) {
        return SpouseWithChildrenDto.builder()
                .name(spouseChildrenPair.getLeft()
                        .map(PersonUtils::getDisplayName)
                        .orElse("<no spouse>"))
                .children(spouseChildrenPair.getRight())
                .build();
    }

    public List<PersonDuplicateDto> toPersonDuplicateDto(Collection<PersonComparisonResults> personComparisonResults) {
        return personComparisonResults
                .stream()
                .map(this::toPersonDuplicateDto)
                .toList();
    }

    public PersonDuplicateDto toPersonDuplicateDto(PersonComparisonResults personComparisonResults) {
        return PersonDuplicateDto.builder()
                .person(toPersonDto(personComparisonResults.getPerson()))
                .duplicates(toPersonDuplicateCompareDto(personComparisonResults.getResults()))
                .build();
    }

    private List<PersonDuplicateCompareDto> toPersonDuplicateCompareDto(Collection<PersonComparisonResult> personComparisonResults) {
        return personComparisonResults
                .stream()
                .map(this::toPersonDuplicateDto)
                .toList();
    }

    private PersonDuplicateCompareDto toPersonDuplicateDto(PersonComparisonResult personComparisonResult) {
        return PersonDuplicateCompareDto.builder()
                .person(toPersonDto(personComparisonResult.getCompare()))
                .score(personComparisonResult.getScore())
                .build();
    }

}
