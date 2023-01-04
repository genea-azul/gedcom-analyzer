package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.mapper.ObfuscationType;
import com.geneaazul.gedcomanalyzer.mapper.PersonMapper;
import com.geneaazul.gedcomanalyzer.mapper.SearchFamilyMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.repository.SearchFamilyRepository;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyService {

    private final GedcomHolder gedcomHolder;
    private final SearchService searchService;
    private final SearchFamilyRepository searchFamilyRepository;
    private final SearchFamilyMapper searchFamilyMapper;
    private final PersonMapper personMapper;

    @Transactional
    public Optional<Long> persistSearch(SearchFamilyDto searchFamilyDto) {
        return Optional.ofNullable(searchFamilyMapper.toSearchFamilyEntity(searchFamilyDto))
                .map(searchFamilyRepository::save)
                .map(SearchFamily::getId);
    }

    @Transactional
    public void updateSearch(Long searchFamilyId, boolean isMatch) {
        searchFamilyRepository
                .findById(searchFamilyId)
                .ifPresent(searchFamily -> searchFamily.setIsMatch(isMatch));
    }

    public SearchFamilyResultDto search(SearchFamilyDto searchFamilyDto) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom()
                .orElseThrow(() -> new IllegalStateException("Server is starting, please try again."));

        List<EnrichedPerson> result = new ArrayList<>();

        String individualSurname = Optional.ofNullable(searchFamilyDto.getIndividual())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getFather())
                        .map(SearchPersonDto::getSurname))
                .or(() -> Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                        .map(SearchPersonDto::getSurname))
                .orElse(null);

        String fatherSurname = Optional.ofNullable(searchFamilyDto.getFather())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                        .map(SearchPersonDto::getSurname))
                .or(() -> Optional.ofNullable(searchFamilyDto.getIndividual())
                        .map(SearchPersonDto::getSurname))
                .orElse(null);

        String motherSurname = Optional.ofNullable(searchFamilyDto.getMother())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getMaternalGrandfather())
                        .map(SearchPersonDto::getSurname))
                .or(() -> Optional.ofNullable(searchFamilyDto.getIndividual())
                        .map(SearchPersonDto::getSurname))
                .orElse(null);

        String paternalGrandfatherSurname = Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getFather())
                        .map(SearchPersonDto::getSurname))
                .or(() -> Optional.ofNullable(searchFamilyDto.getIndividual())
                        .map(SearchPersonDto::getSurname))
                .orElse(null);

        String paternalGrandmotherSurname = Optional.ofNullable(searchFamilyDto.getPaternalGrandmother())
                .map(SearchPersonDto::getSurname)
                .orElse(null);

        String maternalGrandfatherSurname = Optional.ofNullable(searchFamilyDto.getMaternalGrandfather())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getMother())
                        .map(SearchPersonDto::getSurname))
                .orElse(null);

        String maternalGrandmotherSurname = Optional.ofNullable(searchFamilyDto.getMaternalGrandmother())
                .map(SearchPersonDto::getSurname)
                .orElse(null);

        /*
         * Individual
         */

        result.addAll(searchPersons(
                searchFamilyDto.getIndividual(),
                individualSurname,
                searchFamilyDto.getFather(),
                fatherSurname,
                searchFamilyDto.getMother(),
                motherSurname,
                gedcom));

        /*
         * Parents
         */

        result.addAll(searchCouples(
                searchFamilyDto.getFather(),
                fatherSurname,
                searchFamilyDto.getMother(),
                motherSurname,
                gedcom));

        result.addAll(searchPersons(
                searchFamilyDto.getFather(),
                fatherSurname,
                searchFamilyDto.getPaternalGrandfather(),
                paternalGrandfatherSurname,
                searchFamilyDto.getPaternalGrandmother(),
                paternalGrandmotherSurname,
                gedcom));

        result.addAll(searchPersons(
                searchFamilyDto.getMother(),
                motherSurname,
                searchFamilyDto.getMaternalGrandfather(),
                maternalGrandfatherSurname,
                searchFamilyDto.getMaternalGrandmother(),
                maternalGrandmotherSurname,
                gedcom));

        /*
         * Paternal grandparents
         */

        result.addAll(searchCouples(
                searchFamilyDto.getPaternalGrandfather(),
                paternalGrandfatherSurname,
                searchFamilyDto.getPaternalGrandmother(),
                paternalGrandmotherSurname,
                gedcom));

        result.addAll(searchPersons(
                searchFamilyDto.getPaternalGrandfather(),
                paternalGrandfatherSurname,
                gedcom));

        result.addAll(searchPersons(
                searchFamilyDto.getPaternalGrandmother(),
                paternalGrandmotherSurname,
                gedcom));

        /*
         * Maternal grandparents
         */

        result.addAll(searchCouples(
                searchFamilyDto.getMaternalGrandfather(),
                maternalGrandfatherSurname,
                searchFamilyDto.getMaternalGrandmother(),
                maternalGrandmotherSurname,
                gedcom));

        result.addAll(searchPersons(
                searchFamilyDto.getMaternalGrandfather(),
                maternalGrandfatherSurname,
                gedcom));

        result.addAll(searchPersons(
                searchFamilyDto.getMaternalGrandmother(),
                maternalGrandmotherSurname,
                gedcom));

        result = result
                .stream()
                .filter(distinctByKey(EnrichedPerson::getId))
                .toList();

        List<PersonDto> people = personMapper.toPersonDto(result, ObfuscationType.SKIP_MAIN_PERSON_NAME);

        return SearchFamilyResultDto.builder()
                .people(people)
                .build();
    }

    private List<EnrichedPerson> searchPersons(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String personSurname,
            EnrichedGedcom gedcom) {
        return searchPersons(
                searchPerson,
                personSurname,
                null,
                null,
                null,
                null,
                gedcom);
    }

    private List<EnrichedPerson> searchPersons(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String personSurname,
            @Nullable SearchPersonDto searchFather,
            @Nullable String fatherSurname,
            @Nullable SearchPersonDto searchMother,
            @Nullable String motherSurname,
            EnrichedGedcom gedcom) {

        if (searchPerson == null || personSurname == null) {
            return List.of();
        }

        List<EnrichedPerson> yearOfBirthResult = searchService.findPersonsByNameAndYearOfBirth(
                searchPerson.getGivenName(),
                personSurname,
                searchPerson.getSex(),
                searchPerson.getYearOfBirth(),
                gedcom);

        List<EnrichedPerson> yearOfDeathResult = searchService.findPersonsByNameAndYearOfDeath(
                searchPerson.getGivenName(),
                personSurname,
                searchPerson.getSex(),
                searchPerson.getYearOfDeath(),
                gedcom);

        List<EnrichedPerson> parentsResult = searchService.findPersonsByNameAndParentsNames(
                searchPerson.getGivenName(),
                personSurname,
                searchPerson.getSex(),
                Optional.ofNullable(searchFather)
                        .map(SearchPersonDto::getGivenName)
                        .orElse(null),
                fatherSurname,
                Optional.ofNullable(searchFather)
                        .map(SearchPersonDto::getSex)
                        .orElse(null),
                Optional.ofNullable(searchMother)
                        .map(SearchPersonDto::getGivenName)
                        .orElse(null),
                motherSurname,
                Optional.ofNullable(searchMother)
                        .map(SearchPersonDto::getSex)
                        .orElse(null),
                gedcom);

        return Stream
                .of(
                        yearOfBirthResult,
                        yearOfDeathResult,
                        parentsResult)
                .flatMap(List::stream)
                .toList();
    }

    private List<EnrichedPerson> searchCouples(
            @Nullable SearchPersonDto spouse1,
            @Nullable String spouse1Surname,
            @Nullable SearchPersonDto spouse2,
            @Nullable String spouse2Surname,
            EnrichedGedcom gedcom) {

        if (spouse1 == null || spouse1Surname == null || spouse2 == null || spouse2Surname == null) {
            return List.of();
        }

        List<EnrichedPerson> spousesResult1 = searchService.findPersonsByNameAndSpouseName(
                spouse1.getGivenName(),
                spouse1Surname,
                spouse1.getSex(),
                spouse2.getGivenName(),
                spouse2Surname,
                spouse2.getSex(),
                gedcom);

        List<EnrichedPerson> spousesResult2 = searchService.findPersonsByNameAndSpouseName(
                spouse2.getGivenName(),
                spouse2Surname,
                spouse2.getSex(),
                spouse1.getGivenName(),
                spouse1Surname,
                spouse1.getSex(),
                gedcom);

        return Stream
                .of(
                        spousesResult1,
                        spousesResult2)
                .flatMap(List::stream)
                .toList();
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}
