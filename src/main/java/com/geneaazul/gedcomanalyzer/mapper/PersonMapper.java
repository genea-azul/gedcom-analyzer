package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.domain.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.domain.PersonComparisonResult;
import com.geneaazul.gedcomanalyzer.domain.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.PersonDto;
import com.geneaazul.gedcomanalyzer.model.PersonDuplicateCompareDto;
import com.geneaazul.gedcomanalyzer.model.PersonDuplicateDto;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PersonMapper {

    public List<PersonDto> toPersonDto(List<EnrichedPerson> persons) {
        return persons
                .stream()
                .map(this::toPersonDto)
                .toList();
    }

    public PersonDto toPersonDto(EnrichedPerson person) {
        return PersonDto.builder()
                .displayName(person.getDisplayName())
                .build();
    }

    public List<PersonDuplicateDto> toPersonDuplicateDto(List<PersonComparisonResults> personComparisonResults) {
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

    private List<PersonDuplicateCompareDto> toPersonDuplicateCompareDto(List<PersonComparisonResult> personComparisonResults) {
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
