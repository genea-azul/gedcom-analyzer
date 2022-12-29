package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.domain.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.domain.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.mapper.PersonMapper;
import com.geneaazul.gedcomanalyzer.model.PersonDto;
import com.geneaazul.gedcomanalyzer.model.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.SearchPersonDto;

import org.springframework.stereotype.Service;

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
    private final PersonMapper personMapper;

    public SearchFamilyResultDto search(SearchFamilyDto searchFamilyDto) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();

        List<EnrichedPerson> result = new ArrayList<>();

        if (searchFamilyDto.getIndividual() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getIndividual().getGivenName(),
                    searchFamilyDto.getIndividual().getSurname(),
                    searchFamilyDto.getIndividual().getSex(),
                    searchFamilyDto.getIndividual().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getIndividual().getGivenName(),
                    searchFamilyDto.getIndividual().getSurname(),
                    searchFamilyDto.getIndividual().getSex(),
                    searchFamilyDto.getIndividual().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);

            if (searchFamilyDto.getFather() != null || searchFamilyDto.getMother() != null) {
                List<EnrichedPerson> individualResultC = searchService.findPersonsByNameAndParentsNames(
                        searchFamilyDto.getIndividual().getGivenName(),
                        searchFamilyDto.getIndividual().getSurname(),
                        searchFamilyDto.getIndividual().getSex(),
                        Optional.ofNullable(searchFamilyDto.getFather())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getFather())
                                .map(SearchPersonDto::getSurname)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getMother())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getMother())
                                .map(SearchPersonDto::getSurname)
                                .orElse(null),
                        gedcom);
                result.addAll(individualResultC);
            }
        }

        if (searchFamilyDto.getFather() != null && searchFamilyDto.getMother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getFather().getGivenName(),
                    searchFamilyDto.getFather().getSurname(),
                    searchFamilyDto.getFather().getSex(),
                    searchFamilyDto.getMother().getGivenName(),
                    searchFamilyDto.getMother().getSurname(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getMother().getGivenName(),
                    searchFamilyDto.getMother().getSurname(),
                    searchFamilyDto.getMother().getSex(),
                    searchFamilyDto.getFather().getGivenName(),
                    searchFamilyDto.getFather().getSurname(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        if (searchFamilyDto.getFather() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getFather().getGivenName(),
                    searchFamilyDto.getFather().getSurname(),
                    searchFamilyDto.getFather().getSex(),
                    searchFamilyDto.getFather().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getFather().getGivenName(),
                    searchFamilyDto.getFather().getSurname(),
                    searchFamilyDto.getFather().getSex(),
                    searchFamilyDto.getFather().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);

            if (searchFamilyDto.getPaternalGrandfather() != null || searchFamilyDto.getPaternalGrandmother() != null) {
                List<EnrichedPerson> individualResultC = searchService.findPersonsByNameAndParentsNames(
                        searchFamilyDto.getFather().getGivenName(),
                        searchFamilyDto.getFather().getSurname(),
                        searchFamilyDto.getFather().getSex(),
                        Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getPaternalGrandfather())
                                .map(SearchPersonDto::getSurname)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getPaternalGrandmother())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getPaternalGrandmother())
                                .map(SearchPersonDto::getSurname)
                                .orElse(null),
                        gedcom);
                result.addAll(individualResultC);
            }
        }

        if (searchFamilyDto.getMother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getMother().getGivenName(),
                    searchFamilyDto.getMother().getSurname(),
                    searchFamilyDto.getMother().getSex(),
                    searchFamilyDto.getMother().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getMother().getGivenName(),
                    searchFamilyDto.getMother().getSurname(),
                    searchFamilyDto.getMother().getSex(),
                    searchFamilyDto.getMother().getYearOfDeath(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);

            if (searchFamilyDto.getMaternalGrandfather() != null || searchFamilyDto.getMaternalGrandmother() != null) {
                List<EnrichedPerson> individualResultC = searchService.findPersonsByNameAndParentsNames(
                        searchFamilyDto.getMother().getGivenName(),
                        searchFamilyDto.getMother().getSurname(),
                        searchFamilyDto.getMother().getSex(),
                        Optional.ofNullable(searchFamilyDto.getMaternalGrandfather())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getMaternalGrandfather())
                                .map(SearchPersonDto::getSurname)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getMaternalGrandmother())
                                .map(SearchPersonDto::getGivenName)
                                .orElse(null),
                        Optional.ofNullable(searchFamilyDto.getMaternalGrandmother())
                                .map(SearchPersonDto::getSurname)
                                .orElse(null),
                        gedcom);
                result.addAll(individualResultC);
            }
        }

        if (searchFamilyDto.getPaternalGrandfather() != null && searchFamilyDto.getPaternalGrandmother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getPaternalGrandfather().getGivenName(),
                    searchFamilyDto.getPaternalGrandfather().getSurname(),
                    searchFamilyDto.getPaternalGrandfather().getSex(),
                    searchFamilyDto.getPaternalGrandmother().getGivenName(),
                    searchFamilyDto.getPaternalGrandmother().getSurname(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getPaternalGrandmother().getGivenName(),
                    searchFamilyDto.getPaternalGrandmother().getSurname(),
                    searchFamilyDto.getPaternalGrandmother().getSex(),
                    searchFamilyDto.getPaternalGrandfather().getGivenName(),
                    searchFamilyDto.getPaternalGrandfather().getSurname(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        if (searchFamilyDto.getMaternalGrandfather() != null && searchFamilyDto.getMaternalGrandmother() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getMaternalGrandfather().getGivenName(),
                    searchFamilyDto.getMaternalGrandfather().getSurname(),
                    searchFamilyDto.getMaternalGrandfather().getSex(),
                    searchFamilyDto.getMaternalGrandmother().getGivenName(),
                    searchFamilyDto.getMaternalGrandmother().getSurname(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndSpouseName(
                    searchFamilyDto.getMaternalGrandmother().getGivenName(),
                    searchFamilyDto.getMaternalGrandmother().getSurname(),
                    searchFamilyDto.getMaternalGrandmother().getSex(),
                    searchFamilyDto.getMaternalGrandfather().getGivenName(),
                    searchFamilyDto.getMaternalGrandfather().getSurname(),
                    gedcom);
            result.addAll(individualResultA);
            result.addAll(individualResultB);
        }

        if (searchFamilyDto.getPaternalGrandfather() != null) {
            List<EnrichedPerson> individualResultA = searchService.findPersonsByNameAndYearOfBirth(
                    searchFamilyDto.getPaternalGrandfather().getGivenName(),
                    searchFamilyDto.getPaternalGrandfather().getSurname(),
                    searchFamilyDto.getPaternalGrandfather().getSex(),
                    searchFamilyDto.getPaternalGrandfather().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getPaternalGrandfather().getGivenName(),
                    searchFamilyDto.getPaternalGrandfather().getSurname(),
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
                    searchFamilyDto.getMaternalGrandfather().getSurname(),
                    searchFamilyDto.getMaternalGrandfather().getSex(),
                    searchFamilyDto.getMaternalGrandfather().getYearOfBirth(),
                    gedcom);
            List<EnrichedPerson> individualResultB = searchService.findPersonsByNameAndYearOfDeath(
                    searchFamilyDto.getMaternalGrandfather().getGivenName(),
                    searchFamilyDto.getMaternalGrandfather().getSurname(),
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

        List<PersonDto> people = personMapper.toPersonDto(result);

        return SearchFamilyResultDto.builder()
                .people(people)
                .build();
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}
