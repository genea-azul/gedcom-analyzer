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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

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

        String maternalGrandfatherSurname = Optional.ofNullable(searchFamilyDto.getMaternalGrandfather())
                .map(SearchPersonDto::getSurname)
                .or(() -> Optional.ofNullable(searchFamilyDto.getMother())
                        .map(SearchPersonDto::getSurname))
                .orElse(null);

        if (searchFamilyDto.getIndividual() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getIndividual().getGivenName(),
                    individualSurname,
                    searchFamilyDto.getIndividual().getSex(),
                    searchFamilyDto.getIndividual().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getIndividual().getGivenName(),
                    individualSurname,
                    searchFamilyDto.getIndividual().getSex(),
                    searchFamilyDto.getIndividual().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);

            if (searchFamilyDto.getFather() != null || searchFamilyDto.getMother() != null) {
                List<EnrichedPerson> individualResultC = searchService.findPersonsByNameAndParentsNames(
                        searchFamilyDto.getIndividual().getGivenName(),
                        individualSurname,
                        searchFamilyDto.getIndividual().getSex(),
                        Optional.ofNullable(searchFamilyDto.getFather())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        fatherSurname,
                        Optional.ofNullable(searchFamilyDto.getFather())
                                .map(SearchPersonDto::getSex)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getMother())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        motherSurname,
                        Optional.ofNullable(searchFamilyDto.getMother())
                                .map(SearchPersonDto::getSex)
                                .orElse(null),
                        gedcom);
                result.addAll(individualResultC);
            }
        }

        if (searchFamilyDto.getFather() != null && searchFamilyDto.getMother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getFather().getGivenName(),
                    fatherSurname,
                    searchFamilyDto.getFather().getSex(),
                    searchFamilyDto.getMother().getGivenName(),
                    motherSurname,
                    searchFamilyDto.getMother().getSex(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getMother().getGivenName(),
                    motherSurname,
                    searchFamilyDto.getMother().getSex(),
                    searchFamilyDto.getFather().getGivenName(),
                    fatherSurname,
                    searchFamilyDto.getFather().getSex(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        if (searchFamilyDto.getFather() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getFather().getGivenName(),
                    fatherSurname,
                    searchFamilyDto.getFather().getSex(),
                    searchFamilyDto.getFather().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getFather().getGivenName(),
                    fatherSurname,
                    searchFamilyDto.getFather().getSex(),
                    searchFamilyDto.getFather().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);

            if (searchFamilyDto.getPaternalGrandfather() != null || searchFamilyDto.getPaternalGrandmother() != null) {
                List<EnrichedPerson> individualResultC = searchService.findPersonsByNameAndParentsNames(
                        searchFamilyDto.getFather().getGivenName(),
                        fatherSurname,
                        searchFamilyDto.getFather().getSex(),
                        Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        paternalGrandfatherSurname,
                        Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                                .map(SearchPersonDto::getSex)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getPaternalGrandmother())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getPaternalGrandmother())
                                .map(SearchPersonDto::getSurname)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getPaternalGrandmother())
                                .map(SearchPersonDto::getSex)
                                .orElse(null),
                        gedcom);
                result.addAll(individualResultC);
            }
        }

        if (searchFamilyDto.getMother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getMother().getGivenName(),
                    motherSurname,
                    searchFamilyDto.getMother().getSex(),
                    searchFamilyDto.getMother().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getMother().getGivenName(),
                    motherSurname,
                    searchFamilyDto.getMother().getSex(),
                    searchFamilyDto.getMother().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);

            if (searchFamilyDto.getMaternalGrandfather() != null || searchFamilyDto.getMaternalGrandmother() != null) {
                List<EnrichedPerson> individualResultC = searchService.findPersonsByNameAndParentsNames(
                        searchFamilyDto.getMother().getGivenName(),
                        motherSurname,
                        searchFamilyDto.getMother().getSex(),
                        Optional.ofNullable(searchFamilyDto.getMaternalGrandfather())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        maternalGrandfatherSurname,
                        Optional.ofNullable(searchFamilyDto.getMaternalGrandfather())
                                .map(SearchPersonDto::getSex)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getMaternalGrandmother())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getMaternalGrandmother())
                                .map(SearchPersonDto::getSurname)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getMaternalGrandmother())
                                .map(SearchPersonDto::getSex)
                                .orElse(null),
                        gedcom);
                result.addAll(individualResultC);
            }
        }

        if (searchFamilyDto.getPaternalGrandfather() != null && searchFamilyDto.getPaternalGrandmother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getPaternalGrandfather().getGivenName(),
                    paternalGrandfatherSurname,
                    searchFamilyDto.getPaternalGrandfather().getSex(),
                    searchFamilyDto.getPaternalGrandmother().getGivenName(),
                    searchFamilyDto.getPaternalGrandmother().getSurname(),
                    searchFamilyDto.getPaternalGrandmother().getSex(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getPaternalGrandmother().getGivenName(),
                    searchFamilyDto.getPaternalGrandmother().getSurname(),
                    searchFamilyDto.getPaternalGrandmother().getSex(),
                    searchFamilyDto.getPaternalGrandfather().getGivenName(),
                    paternalGrandfatherSurname,
                    searchFamilyDto.getPaternalGrandfather().getSex(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        if (searchFamilyDto.getMaternalGrandfather() != null && searchFamilyDto.getMaternalGrandmother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getMaternalGrandfather().getGivenName(),
                    maternalGrandfatherSurname,
                    searchFamilyDto.getMaternalGrandfather().getSex(),
                    searchFamilyDto.getMaternalGrandmother().getGivenName(),
                    searchFamilyDto.getMaternalGrandmother().getSurname(),
                    searchFamilyDto.getMaternalGrandmother().getSex(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getMaternalGrandmother().getGivenName(),
                    searchFamilyDto.getMaternalGrandmother().getSurname(),
                    searchFamilyDto.getMaternalGrandmother().getSex(),
                    searchFamilyDto.getMaternalGrandfather().getGivenName(),
                    maternalGrandfatherSurname,
                    searchFamilyDto.getMaternalGrandfather().getSex(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        if (searchFamilyDto.getPaternalGrandfather() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getPaternalGrandfather().getGivenName(),
                    paternalGrandfatherSurname,
                    searchFamilyDto.getPaternalGrandfather().getSex(),
                    searchFamilyDto.getPaternalGrandfather().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getPaternalGrandfather().getGivenName(),
                    paternalGrandfatherSurname,
                    searchFamilyDto.getPaternalGrandfather().getSex(),
                    searchFamilyDto.getPaternalGrandfather().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        if (searchFamilyDto.getPaternalGrandmother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getPaternalGrandmother().getGivenName(),
                    searchFamilyDto.getPaternalGrandmother().getSurname(),
                    searchFamilyDto.getPaternalGrandmother().getSex(),
                    searchFamilyDto.getPaternalGrandmother().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getPaternalGrandmother().getGivenName(),
                    searchFamilyDto.getPaternalGrandmother().getSurname(),
                    searchFamilyDto.getPaternalGrandmother().getSex(),
                    searchFamilyDto.getPaternalGrandmother().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        if (searchFamilyDto.getMaternalGrandfather() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getMaternalGrandfather().getGivenName(),
                    maternalGrandfatherSurname,
                    searchFamilyDto.getMaternalGrandfather().getSex(),
                    searchFamilyDto.getMaternalGrandfather().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getMaternalGrandfather().getGivenName(),
                    maternalGrandfatherSurname,
                    searchFamilyDto.getMaternalGrandfather().getSex(),
                    searchFamilyDto.getMaternalGrandfather().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        if (searchFamilyDto.getMaternalGrandmother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getMaternalGrandmother().getGivenName(),
                    searchFamilyDto.getMaternalGrandmother().getSurname(),
                    searchFamilyDto.getMaternalGrandmother().getSex(),
                    searchFamilyDto.getMaternalGrandmother().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getMaternalGrandmother().getGivenName(),
                    searchFamilyDto.getMaternalGrandmother().getSurname(),
                    searchFamilyDto.getMaternalGrandmother().getSex(),
                    searchFamilyDto.getMaternalGrandmother().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        result = result
                .stream()
                .filter(distinctByKey(EnrichedPerson::getId))
                .toList();

        List<PersonDto> people = personMapper.toPersonDto(result, ObfuscationType.SKIP_MAIN_PERSON_NAME);

        return SearchFamilyResultDto.builder()
                .people(people)
                .build();
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}
