package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.EnrichedSpouseWithChildren;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResult;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.AncestryGenerationsDto;
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

    public List<PersonDto> toPersonDto(Collection<EnrichedPerson> persons) {
        return persons
                .stream()
                .map(this::toPersonDto)
                .toList();
    }

    public PersonDto toPersonDto(EnrichedPerson person) {
        return toPersonDto(
                person,
                ObfuscationType.NONE,
                ep -> 1,
                ep -> List.of(),
                ep -> AncestryGenerations.empty(),
                ep -> Optional.empty());
    }

    public List<PersonDto> toPersonDto(
            Collection<EnrichedPerson> persons,
            ObfuscationType obfuscationType,
            Function<EnrichedPerson, Integer> numberOfPeopleInTreeResolver,
            Function<EnrichedPerson, List<String>> ancestryCountriesResolver,
            Function<EnrichedPerson, AncestryGenerations> ancestryGenerationsResolver,
            Function<EnrichedPerson, Optional<Relationship>> maxDistantRelationshipResolver) {
        return persons
                .stream()
                .map(person -> toPersonDto(
                        person,
                        obfuscationType,
                        numberOfPeopleInTreeResolver,
                        ancestryCountriesResolver,
                        ancestryGenerationsResolver,
                        maxDistantRelationshipResolver))
                .toList();
    }

    public PersonDto toPersonDto(
            EnrichedPerson person,
            ObfuscationType obfuscationType,
            Function<EnrichedPerson, Integer> numberOfPeopleInTreeResolver,
            Function<EnrichedPerson, List<String>> ancestryCountriesResolver,
            Function<EnrichedPerson, AncestryGenerations> ancestryGenerationsResolver,
            Function<EnrichedPerson, Optional<Relationship>> maxDistantRelationshipResolver) {

        boolean obfuscateLiving = obfuscationType != ObfuscationType.NONE;
        boolean obfuscateName = obfuscateLiving && obfuscationType != ObfuscationType.SKIP_MAIN_PERSON_NAME;

        List<SpouseWithChildrenDto> spouses = toSpouseWithChildrenDto(
                person.getSpousesWithChildren(),
                obfuscateLiving,
                person.isAlive());

        Integer numberOfPeopleInTree = numberOfPeopleInTreeResolver.apply(person);
        List<String> ancestryCountries = ancestryCountriesResolver.apply(person);
        AncestryGenerations ancestryGenerations = ancestryGenerationsResolver.apply(person);
        Optional<Relationship> maybeMaxDistantRelationship = maxDistantRelationshipResolver.apply(person);

        AncestryGenerationsDto ancestryGenerationsDto = AncestryGenerationsDto.builder()
                .ascending(ancestryGenerations.ascending())
                .descending(ancestryGenerations.descending())
                .directDescending(ancestryGenerations.directDescending())
                .build();

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
                                        parentWithReference.getPerson(),
                                        obfuscateLiving && (person.isAlive() || parentWithReference.getPerson().isAlive())))
                                .sex(parentWithReference.getPerson().getSex())
                                .referenceType(parentWithReference.getReferenceType().orElse(null))
                                .build())
                        .toList())
                .spouses(spouses)
                .numberOfPeopleInTree(numberOfPeopleInTree)
                .ancestryCountries(ancestryCountries)
                .ancestryGenerations(ancestryGenerationsDto)
                .maxDistantRelationship(maybeMaxDistantRelationship
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
                                        childWithReference.getPerson(),
                                        obfuscateLiving && (mainPersonIsAlive || spouseIsAlive || childWithReference.getPerson().isAlive())))
                                .sex(childWithReference.getPerson().getSex())
                                .referenceType(childWithReference.getReferenceType().orElse(null))
                                .build())
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
