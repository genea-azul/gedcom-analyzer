package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.EnrichedSpouseWithChildren;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResult;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateCompareDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonWithReferenceDto;
import com.geneaazul.gedcomanalyzer.model.dto.SpouseWithChildrenDto;
import com.geneaazul.gedcomanalyzer.utils.DateUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PersonMapper {

    private final RelationshipMapper relationshipMapper;
    private final AncestryGenerationsMapper ancestryGenerationsMapper;

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
                person.getSpousesWithChildren(),
                obfuscateLiving,
                person.isAlive());

        return PersonDto.builder()
                .uuid(person.getUuid())
                .sex(person.getSex())
                .isAlive(person.isAlive())
                .name(PersonUtils.obfuscateName(person, obfuscateName && person.isAlive()))
                .aka(person.getAka()
                        .filter(aka -> !(obfuscateName && person.isAlive()))
                        .orElse(null))
                .dateOfBirth(person.getDateOfBirth()
                        .map(date -> DateUtils.obfuscateDate(date, obfuscateLiving && person.isAlive()))
                        .orElse(null))
                .placeOfBirth(person.getCountryOfBirth()
                        .orElse(null))
                .dateOfDeath(person.getDateOfDeath()
                        .map(Date::format)
                        .orElse(null))
                .parents(person.getParentsWithReference()
                        .stream()
                        .map(parentWithReference -> PersonWithReferenceDto.builder()
                                .name(PersonUtils.obfuscateName(
                                        parentWithReference.person(),
                                        obfuscateLiving && (person.isAlive() || parentWithReference.person().isAlive())))
                                .sex(parentWithReference.person().getSex())
                                .referenceType(parentWithReference.referenceType().orElse(null))
                                .build())
                        .toList())
                .spouses(spouses)
                .personsCountInTree(person.getPersonsCountInTree())
                .surnamesCountInTree(person.getSurnamesCountInTree())
                .ancestryCountries(person.getAncestryCountries())
                .ancestryGenerations(Optional
                        .ofNullable(person.getAncestryGenerations())
                        .map(ancestryGenerationsMapper::toAncestryGenerationDto)
                        .orElse(null))
                .maxDistantRelationship(Optional
                        .ofNullable(person.getMaxDistantRelationship())
                        .flatMap(Function.identity())
                        .map(maxDistantRelationship -> relationshipMapper.toRelationshipDto(
                                maxDistantRelationship,
                                obfuscateLiving && (person.isAlive() || maxDistantRelationship.person().isAlive())))
                        .orElse(null))
                .build();
    }

    public List<SpouseWithChildrenDto> toSpouseWithChildrenDto(
            List<EnrichedSpouseWithChildren> spousesWithChildren,
            boolean obfuscateLiving,
            boolean mainPersonIsAlive) {
        return spousesWithChildren
                .stream()
                .map(spouseWithChildren -> toSpouseWithChildrenDto(spouseWithChildren, obfuscateLiving, mainPersonIsAlive))
                .toList();
    }

    public SpouseWithChildrenDto toSpouseWithChildrenDto(
            EnrichedSpouseWithChildren spouseWithChildren,
            boolean obfuscateLiving,
            boolean mainPersonIsAlive) {

        boolean spouseIsAlive = spouseWithChildren.getSpouse()
                .map(EnrichedPerson::isAlive)
                .orElse(false);

        return SpouseWithChildrenDto.builder()
                .name(PersonUtils.obfuscateSpouseName(
                        spouseWithChildren.getSpouse(),
                        spouse -> obfuscateLiving && (mainPersonIsAlive || spouse.isAlive())))
                .children(spouseWithChildren.getChildrenWithReference()
                        .stream()
                        .map(childWithReference -> PersonWithReferenceDto.builder()
                                .name(PersonUtils.obfuscateName(
                                        childWithReference.person(),
                                        obfuscateLiving && (mainPersonIsAlive || spouseIsAlive || childWithReference.person().isAlive())))
                                .sex(childWithReference.person().getSex())
                                .referenceType(childWithReference.referenceType().orElse(null))
                                .build())
                        .toList())
                .build();
    }

    public List<PersonDuplicateDto> toPersonDuplicateDto(
            Collection<PersonComparisonResults> personComparisonResults,
            ObfuscationType obfuscationType) {
        return personComparisonResults
                .stream()
                .map(personComparisonResult -> toPersonDuplicateDto(personComparisonResult, obfuscationType))
                .toList();
    }

    public PersonDuplicateDto toPersonDuplicateDto(
            PersonComparisonResults personComparisonResults,
            ObfuscationType obfuscationType) {
        return PersonDuplicateDto.builder()
                .person(toPersonDto(personComparisonResults.person(), obfuscationType))
                .duplicates(toPersonDuplicateCompareDto(personComparisonResults.results(), obfuscationType))
                .build();
    }

    private List<PersonDuplicateCompareDto> toPersonDuplicateCompareDto(
            Collection<PersonComparisonResult> personComparisonResults,
            ObfuscationType obfuscationType) {
        return personComparisonResults
                .stream()
                .map(personComparisonResult -> toPersonDuplicateDto(personComparisonResult, obfuscationType))
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
