package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.mapper.ObfuscationType;
import com.geneaazul.gedcomanalyzer.mapper.PersonMapper;
import com.geneaazul.gedcomanalyzer.mapper.SearchFamilyMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.repository.SearchFamilyRepository;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final GedcomHolder gedcomHolder;
    private final PersonService personService;
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

    @Transactional(readOnly = true)
    public List<SearchFamilyDetailsDto> getLatest(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return searchFamilyRepository
                .findAll(pageable)
                .map(searchFamilyMapper::toSearchFamilyDetailsDto)
                .toList();
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
                .map(StringUtils::trimToNull)
                .orElse(null);

        String fatherSurname = Optional.ofNullable(searchFamilyDto.getFather())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                        .map(SearchPersonDto::getSurname))
                .or(() -> Optional.ofNullable(searchFamilyDto.getIndividual())
                        .map(SearchPersonDto::getSurname))
                .map(StringUtils::trimToNull)
                .orElse(null);

        String motherSurname = Optional.ofNullable(searchFamilyDto.getMother())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getMaternalGrandfather())
                        .map(SearchPersonDto::getSurname))
                .or(() -> Optional.ofNullable(searchFamilyDto.getIndividual())
                        .map(SearchPersonDto::getSurname))
                .map(StringUtils::trimToNull)
                .orElse(null);

        String paternalGrandfatherSurname = Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getFather())
                        .map(SearchPersonDto::getSurname))
                .or(() -> Optional.ofNullable(searchFamilyDto.getIndividual())
                        .map(SearchPersonDto::getSurname))
                .map(StringUtils::trimToNull)
                .orElse(null);

        String paternalGrandmotherSurname = Optional.ofNullable(searchFamilyDto.getPaternalGrandmother())
                .map(SearchPersonDto::getSurname)
                .map(StringUtils::trimToNull)
                .orElse(null);

        String maternalGrandfatherSurname = Optional.ofNullable(searchFamilyDto.getMaternalGrandfather())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getMother())
                        .map(SearchPersonDto::getSurname))
                .map(StringUtils::trimToNull)
                .orElse(null);

        String maternalGrandmotherSurname = Optional.ofNullable(searchFamilyDto.getMaternalGrandmother())
                .map(SearchPersonDto::getSurname)
                .map(StringUtils::trimToNull)
                .orElse(null);

        /*
         * Individual
         */

        result.addAll(searchPersonByNameAndYearAndParentsNames(
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

        result.addAll(searchPersonsByNameAndSpouseName(
                searchFamilyDto.getFather(),
                fatherSurname,
                searchFamilyDto.getMother(),
                motherSurname,
                gedcom));

        result.addAll(searchPersonByNameAndYearAndParentsNames(
                searchFamilyDto.getFather(),
                fatherSurname,
                searchFamilyDto.getPaternalGrandfather(),
                paternalGrandfatherSurname,
                searchFamilyDto.getPaternalGrandmother(),
                paternalGrandmotherSurname,
                gedcom));

        result.addAll(searchPersonByNameAndYearAndParentsNames(
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

        result.addAll(searchPersonsByNameAndSpouseName(
                searchFamilyDto.getPaternalGrandfather(),
                paternalGrandfatherSurname,
                searchFamilyDto.getPaternalGrandmother(),
                paternalGrandmotherSurname,
                gedcom));

        result.addAll(searchPersonByNameAndYear(
                searchFamilyDto.getPaternalGrandfather(),
                paternalGrandfatherSurname,
                gedcom));

        result.addAll(searchPersonByNameAndYear(
                searchFamilyDto.getPaternalGrandmother(),
                paternalGrandmotherSurname,
                gedcom));

        /*
         * Maternal grandparents
         */

        result.addAll(searchPersonsByNameAndSpouseName(
                searchFamilyDto.getMaternalGrandfather(),
                maternalGrandfatherSurname,
                searchFamilyDto.getMaternalGrandmother(),
                maternalGrandmotherSurname,
                gedcom));

        result.addAll(searchPersonByNameAndYear(
                searchFamilyDto.getMaternalGrandfather(),
                maternalGrandfatherSurname,
                gedcom));

        result.addAll(searchPersonByNameAndYear(
                searchFamilyDto.getMaternalGrandmother(),
                maternalGrandmotherSurname,
                gedcom));

        result = result
                .stream()
                .filter(distinctByKey(EnrichedPerson::getId))
                .toList();

        Integer potentialResultsCount = null;

        if (result.isEmpty()) {
            List<EnrichedPerson> potentialResults = new ArrayList<>();
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getIndividual(), individualSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getFather(), fatherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getMother(), motherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getPaternalGrandfather(), paternalGrandfatherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getPaternalGrandmother(), paternalGrandmotherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getMaternalGrandfather(), maternalGrandfatherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getMaternalGrandmother(), maternalGrandmotherSurname, gedcom));

            potentialResultsCount = (int) potentialResults
                    .stream()
                    .filter(distinctByKey(EnrichedPerson::getId))
                    .count();
        }

        List<PersonDto> people = personMapper.toPersonDto(
                result,
                ObfuscationType.SKIP_MAIN_PERSON_NAME,
                personService::getAncestryCountries);

        return SearchFamilyResultDto.builder()
                .people(people)
                .potentialResults(potentialResultsCount)
                .build();
    }

    private List<EnrichedPerson> getPotentialResults(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String surname,
            EnrichedGedcom gedcom) {
        if (hasSurnameButMissingDates(searchPerson, surname)) {
            return searchPersonByNameOrSurname(searchPerson, surname, gedcom);
        }
        if (hasAnyDateButMissingGivenName(searchPerson, surname)) {
            return searchPersonBySurnameAndYear(searchPerson, surname, gedcom);
        }
        return List.of();
    }

    private boolean hasSurnameButMissingDates(SearchPersonDto searchPersonDto, String surname) {
        return searchPersonDto != null
                && surname != null
                && searchPersonDto.getYearOfBirth() == null
                && searchPersonDto.getYearOfDeath() == null;
    }

    private boolean hasAnyDateButMissingGivenName(SearchPersonDto searchPersonDto, String surname) {
        return searchPersonDto != null
                && surname != null
                && StringUtils.isBlank(searchPersonDto.getGivenName())
                && (searchPersonDto.getYearOfBirth() != null || searchPersonDto.getYearOfDeath() != null);
    }

    private List<EnrichedPerson> searchPersonByNameOrSurname(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String personSurname,
            EnrichedGedcom gedcom) {

        if (searchPerson == null || personSurname == null) {
            return List.of();
        }

        return StringUtils.isNotBlank(searchPerson.getGivenName())
                ? searchService.findPersonsByName(
                        searchPerson.getGivenName(),
                        personSurname,
                        searchPerson.getSex(),
                        gedcom)
                : searchService.findPersonsBySurname(
                        personSurname,
                        searchPerson.getSex(),
                        gedcom);
    }

    private List<EnrichedPerson> searchPersonBySurnameAndYear(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String personSurname,
            EnrichedGedcom gedcom) {

        if (searchPerson == null || personSurname == null) {
            return List.of();
        }

        List<EnrichedPerson> yearOfBirthResult = searchService.findPersonsBySurnameAndYearOfBirth(
                personSurname,
                searchPerson.getSex(),
                searchPerson.getYearOfBirth(),
                gedcom);

        List<EnrichedPerson> yearOfDeathResult = searchService.findPersonsBySurnameAndYearOfDeath(
                personSurname,
                searchPerson.getSex(),
                searchPerson.getYearOfDeath(),
                gedcom);

        return Stream
                .of(
                        yearOfBirthResult,
                        yearOfDeathResult)
                .flatMap(List::stream)
                .toList();
    }

    private List<EnrichedPerson> searchPersonByNameAndYear(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String personSurname,
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

        return Stream
                .of(
                        yearOfBirthResult,
                        yearOfDeathResult)
                .flatMap(List::stream)
                .toList();
    }

    private List<EnrichedPerson> searchPersonByNameAndYearAndParentsNames(
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

    private List<EnrichedPerson> searchPersonsByNameAndSpouseName(
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
