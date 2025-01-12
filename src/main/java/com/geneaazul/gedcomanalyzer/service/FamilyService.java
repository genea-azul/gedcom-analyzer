package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
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
import com.geneaazul.gedcomanalyzer.repository.SearchConnectionRepository;
import com.geneaazul.gedcomanalyzer.repository.SearchFamilyRepository;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.StreamUtils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private static final int MAX_PAGE_SIZE = 1000;

    private final GedcomHolder gedcomHolder;
    private final PersonService personService;
    private final SearchService searchService;
    private final SearchFamilyRepository searchFamilyRepository;
    private final SearchConnectionRepository searchConnectionRepository;
    private final SearchFamilyMapper searchFamilyMapper;
    private final PersonMapper personMapper;
    private final GedcomAnalyzerProperties properties;

    @Transactional
    public Optional<Long> persistFamilySearch(
            SearchFamilyDto searchFamilyDto,
            boolean isObfuscated,
            @Nullable String clientIpAddress) {
        return Optional.ofNullable(searchFamilyMapper.toSearchFamilyEntity(searchFamilyDto, isObfuscated, clientIpAddress))
                .map(searchFamilyRepository::save)
                .map(SearchFamily::getId);
    }

    @Transactional
    public void updateFamilySearchResult(
            Long searchFamilyId,
            boolean isMatch,
            @Nullable Integer potentialResults,
            @Nullable String errorMessages) {
        searchFamilyRepository
                .findById(searchFamilyId)
                .ifPresent(searchFamily -> {
                    searchFamily.setIsMatch(isMatch);
                    searchFamily.setPotentialResults(isMatch ? null : potentialResults);
                    searchFamily.setErrorMessages(StringUtils.substring(StringUtils.trimToNull(errorMessages), 0, 255));
                });
    }

    @Transactional
    public SearchFamilyDetailsDto updateFamilySearchIsReviewed(Long searchFamilyId, Boolean isReviewed) {
        return searchFamilyRepository
                .findById(searchFamilyId)
                .map(searchFamily -> {
                    searchFamily.setIsReviewed(isReviewed);
                    return searchFamily;
                })
                .map(searchFamilyMapper::toSearchFamilyDetailsDto)
                .orElseThrow(() -> new RuntimeException("SearchFamily not found id=" + searchFamilyId));
    }

    @Transactional
    public SearchFamilyDetailsDto updateFamilySearchIsIgnored(Long searchFamilyId, Boolean isIgnored) {
        return searchFamilyRepository
                .findById(searchFamilyId)
                .map(searchFamily -> {
                    searchFamily.setIsIgnored(isIgnored);
                    return searchFamily;
                })
                .map(searchFamilyMapper::toSearchFamilyDetailsDto)
                .orElseThrow(() -> new RuntimeException("SearchFamily not found id=" + searchFamilyId));
    }

    @Transactional(readOnly = true)
    public List<SearchFamilyDetailsDto> getSearchFamilies(
            @Nullable Boolean isMatch,
            @Nullable Boolean isReviewed,
            @Nullable Boolean isIgnored,
            @Nullable Boolean hasContact,
            int page,
            int size,
            @Nullable Sort sort,
            @Nullable String context) {
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                page,
                size,
                Optional
                        .ofNullable(sort)
                        .orElseGet(Sort::unsorted));

        return searchFamilyRepository
                .findAll(Specification
                        .where(SearchFamilyRepository.isMatch(isMatch))
                        .and(SearchFamilyRepository.isReviewed(isReviewed))
                        .and(SearchFamilyRepository.isIgnored(isIgnored))
                        .and(SearchFamilyRepository.hasContact(hasContact)), pageable)
                .stream()
                .map(searchFamilyMapper::toSearchFamilyDetailsDto)
                .peek(details -> {
                    //noinspection DataFlowIssue
                    if (context != null && !Boolean.TRUE.equals(details.getIsReviewed())) {
                        details.setMarkReviewedLink(context + "/api/search/family/" + details.getId() + "/reviewed");
                    }
                    if (context != null && !Boolean.TRUE.equals(details.getIsIgnored())) {
                        details.setMarkIgnoredLink(context + "/api/search/family/" + details.getId() + "/ignored");
                    }
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchFamilyDetailsDto> getLatest(
            @Nullable Boolean isMatch,
            @Nullable Boolean isReviewed,
            @Nullable Boolean isIgnored,
            @Nullable Boolean hasContact,
            int page,
            int size,
            @Nullable String context) {
        return getSearchFamilies(
                isMatch,
                isReviewed,
                isIgnored,
                hasContact,
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id"),
                context);
    }

    @Transactional(readOnly = true)
    public boolean isAllowedSearch(@Nullable String clientIpAddress) {
        if (clientIpAddress == null) {
            return true;
        }

        OffsetDateTime createDateTo = OffsetDateTime.now();
        OffsetDateTime createDateFrom = createDateTo.minusHours(properties.getMaxClientRequestsHoursThreshold());

        long familyClientRequests = searchFamilyRepository.countByClientIpAddressAndCreateDateBetween(clientIpAddress, createDateFrom, createDateTo);
        long connectionClientRequests = searchConnectionRepository.countByClientIpAddressAndCreateDateBetween(clientIpAddress, createDateFrom, createDateTo);

        boolean isSpecialThresholdClient = properties.getClientsWithSpecialThreshold().contains(clientIpAddress);
        int offset = 1; // Since search is persisted before setting the result we need to skip last persisted one
        return (familyClientRequests + connectionClientRequests - offset) < (isSpecialThresholdClient
                ? properties.getMaxClientRequestsCountSpecialThreshold()
                : properties.getMaxClientRequestsCountThreshold());
    }

    public SearchFamilyResultDto search(SearchFamilyDto searchFamilyDto) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();

        List<EnrichedPerson> result = new ArrayList<>();

        String individualSurname = Optional.ofNullable(searchFamilyDto.getIndividual())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getFather())
                        .map(SearchPersonDto::getSurname))
                .or(() -> Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                        .map(SearchPersonDto::getSurname))
                .map(StringUtils::trimToNull)
                .orElse(null);

        String spouseSurname = Optional.ofNullable(searchFamilyDto.getSpouse())
                .map(SearchPersonDto::getSurname)
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
         * Individual and spouse
         */

        result.addAll(searchPersonByNameAndYearAndParentsNames(
                searchFamilyDto.getIndividual(),
                individualSurname,
                searchFamilyDto.getFather(),
                fatherSurname,
                searchFamilyDto.getMother(),
                motherSurname,
                gedcom));

        result.addAll(searchPersonsByNameAndSpouseName(
                searchFamilyDto.getIndividual(),
                individualSurname,
                searchFamilyDto.getSpouse(),
                spouseSurname,
                gedcom));

        result.addAll(searchPersonByNameAndYear(
                searchFamilyDto.getSpouse(),
                spouseSurname,
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

        /*
         * Process results
         */

        result = result
                .stream()
                .filter(StreamUtils.distinctByKey(EnrichedPerson::getId))
                .peek(person -> personService.setTransientProperties(person, true))
                .toList();

        boolean obfuscateLiving = !properties.isDisableObfuscateLiving()
                && BooleanUtils.isNotFalse(searchFamilyDto.getObfuscateLiving());

        ObfuscationType obfuscationType = obfuscateLiving
                ? ObfuscationType.SKIP_MAIN_PERSON_NAME
                : ObfuscationType.NONE;
        List<PersonDto> people = personMapper.toPersonDto(result, obfuscationType);

        Integer potentialResultsCount = null;

        if (result.isEmpty()) {
            List<EnrichedPerson> potentialResults = new ArrayList<>();
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getIndividual(), individualSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getSpouse(), spouseSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getFather(), fatherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getMother(), motherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getPaternalGrandfather(), paternalGrandfatherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getPaternalGrandmother(), paternalGrandmotherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getMaternalGrandfather(), maternalGrandfatherSurname, gedcom));
            potentialResults.addAll(getPotentialResults(searchFamilyDto.getMaternalGrandmother(), maternalGrandmotherSurname, gedcom));

            potentialResultsCount = (int) potentialResults
                    .stream()
                    .filter(StreamUtils.distinctByKey(EnrichedPerson::getId))
                    .count();
        }

        return SearchFamilyResultDto.builder()
                .people(people)
                .potentialResults(potentialResultsCount)
                .build();
    }

    private List<EnrichedPerson> getPotentialResults(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String personSurname,
            EnrichedGedcom gedcom) {
        if (hasSurnameButMissingDates(searchPerson, personSurname)) {
            return searchPersonByNameOrSurname(searchPerson, personSurname, gedcom);
        }
        if (hasAnyDateAndSurnameButMissingGivenName(searchPerson, personSurname)) {
            return searchPersonBySurnameAndYear(searchPerson, personSurname, gedcom);
        }
        return List.of();
    }

    private boolean hasSurnameButMissingDates(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String personSurname) {
        return searchPerson != null
                && personSurname != null
                && searchPerson.getYearOfBirth() == null
                && searchPerson.getYearOfDeath() == null;
    }

    private boolean hasAnyDateAndSurnameButMissingGivenName(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String personSurname) {
        return searchPerson != null
                && personSurname != null
                && StringUtils.isBlank(searchPerson.getGivenName())
                && (searchPerson.getYearOfBirth() != null || searchPerson.getYearOfDeath() != null);
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

}
