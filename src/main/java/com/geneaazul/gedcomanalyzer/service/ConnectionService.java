package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.domain.SearchConnection;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.mapper.SearchConnectionMapper;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.ConnectionDto;
import com.geneaazul.gedcomanalyzer.model.dto.RelationshipDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchConnectionDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchConnectionDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchConnectionResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.repository.SearchConnectionRepository;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.PathUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;
import com.geneaazul.gedcomanalyzer.utils.PlaceUtils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private static final int MAX_PAGE_SIZE = 1000;

    private final GedcomHolder gedcomHolder;
    private final PersonService personService;
    private final SearchService searchService;
    private final SearchConnectionRepository searchConnectionRepository;
    private final SearchConnectionMapper searchConnectionMapper;
    private final RelationshipMapper relationshipMapper;

    @Transactional
    public Optional<Long> persistConnectionSearch(
            SearchConnectionDto searchConnectionDto,
            @Nullable String clientIpAddress) {
        return Optional.ofNullable(searchConnectionMapper.toSearchConnectionEntity(searchConnectionDto, clientIpAddress))
                .map(searchConnectionRepository::save)
                .map(SearchConnection::getId);
    }

    @Transactional
    public void updateConnectionSearchResult(
            Long searchConnectionId,
            boolean isMatch,
            @Nullable Integer distance,
            @Nullable String errorMessages) {
        searchConnectionRepository
                .findById(searchConnectionId)
                .ifPresent(searchConnection -> {
                    searchConnection.setIsMatch(isMatch);
                    searchConnection.setDistance(distance);
                    searchConnection.setErrorMessages(StringUtils.substring(StringUtils.trimToNull(errorMessages), 0, 255));
                });
    }

    @Transactional
    public SearchConnectionDetailsDto updateConnectionSearchIsReviewed(Long searchConnectionId, Boolean isReviewed) {
        return searchConnectionRepository
                .findById(searchConnectionId)
                .map(searchConnection -> {
                    searchConnection.setIsReviewed(isReviewed);
                    return searchConnection;
                })
                .map(searchConnectionMapper::toSearchConnectionDetailsDto)
                .orElseThrow(() -> new RuntimeException("SearchConnection not found id=" + searchConnectionId));
    }

    public SearchConnectionResultDto search(SearchConnectionDto searchConnectionDto) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();

        String person1Surname = Optional.ofNullable(searchConnectionDto.getPerson1())
                .map(SearchPersonDto::getSurname)
                .map(StringUtils::trimToNull)
                .orElse(null);

        String person2Surname = Optional.ofNullable(searchConnectionDto.getPerson2())
                .map(SearchPersonDto::getSurname)
                .map(StringUtils::trimToNull)
                .orElse(null);

        List<EnrichedPerson> person1Result = searchPersonByNameAndYear(
                searchConnectionDto.getPerson1(),
                person1Surname,
                gedcom);

        List<EnrichedPerson> person2Result = searchPersonByNameAndYear(
                searchConnectionDto.getPerson2(),
                person2Surname,
                gedcom);

        List<String> errors = getPossibleErrors(person1Result, person2Result);

        /*
         * Process connections
         */

        List<ConnectionDto> connections = new ArrayList<>();

        if (errors.isEmpty()) {
            EnrichedPerson person1 = person1Result.getFirst();
            Pair<Map<Integer, Integer>, Map<Integer, List<Integer>>> distancesAndPaths = PathUtils
                    .calculateShortestPathFromSource(gedcom, person1, true, true);

            EnrichedPerson person2 = person2Result.getFirst();
            List<Integer> shortestPath = distancesAndPaths.getRight().getOrDefault(person2.getId(), List.of());

            for (int i = 0; i < shortestPath.size() - 1; i++) {
                EnrichedPerson personA = gedcom.getPersonById(shortestPath.get(i));
                EnrichedPerson personB = gedcom.getPersonById(shortestPath.get(i + 1));
                Relationship relationship = personService.getRelationshipBetween(personB, personA);
                RelationshipDto relationshipDto = relationshipMapper.toRelationshipDto(relationship, false);
                FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);
                if (i == 0) {
                    Pair<String, String> personAInfo = displayPersonInfo(personA, false);
                    connections.add(
                            ConnectionDto.builder()
                                    .relationship(null)
                                    .personName(personAInfo.getLeft())
                                    .personData(personAInfo.getRight())
                                    .build());
                }
                Pair<String, String> personBInfo = displayPersonInfo(personB, false);
                connections.add(
                        ConnectionDto.builder()
                                .relationship(formattedRelationship.relationshipDesc())
                                .personName(personBInfo.getLeft())
                                .personData(personBInfo.getRight())
                                .build());
            }
        }

        return SearchConnectionResultDto.builder()
                .connections(connections)
                .errors(errors)
                .build();
    }

    private static @NotNull List<String> getPossibleErrors(List<EnrichedPerson> person1Result, List<EnrichedPerson> person2Result) {
        List<String> errors = new ArrayList<>();
        if (person1Result.isEmpty()) {
            errors.add("CONNECTIONS-PERSON-1-NOT-FOUND");
        } else if (person1Result.size() > 1) {
            errors.add("CONNECTIONS-PERSON-1-AMBIGUOUS");
        }
        if (person2Result.isEmpty()) {
            errors.add("CONNECTIONS-PERSON-2-NOT-FOUND");
        } else if (person2Result.size() > 1) {
            errors.add("CONNECTIONS-PERSON-2-AMBIGUOUS");
        }
        // Check is not same person
        if (errors.isEmpty() && person1Result.getFirst().getId().equals(person2Result.getFirst().getId())) {
            errors.add("CONNECTIONS-SAME-PERSON");
        }
        return errors;
    }

    private Pair<String, String> displayPersonInfo(
            EnrichedPerson person,
            boolean obfuscateCondition) {
        boolean displayDate = !obfuscateCondition
                && person
                        .getDateOfBirth()
                        .filter(date -> date.getOperator() == null
                                || date.getOperator() != Date.Operator.AFT && date.getOperator() != Date.Operator.BEF)
                        .isPresent();
        boolean displayPlace = person.getPlaceOfBirth().isPresent();
        String displayName = PersonUtils.obfuscateName(person, obfuscateCondition);
        if (!displayDate && !displayPlace) {
            return Pair.of(displayName, null);
        }
        if (!displayDate) {
            String countryStr = person
                    .getPlaceOfBirth()
                    .map(Place::country)
                    .map(PlaceUtils::adjustCountryForReport)
                    .map(country -> country.substring(0, 3))
                    .orElseThrow();
            return Pair.of(displayName, countryStr);
        }
        String dateStr = person
                .getDateOfBirth()
                .map(date -> ((date.getOperator() == Date.Operator.EST || date.getOperator() == Date.Operator.ABT)
                        ? "aprox. " + date.getYear()
                        : String.valueOf(date.getYear())))
                .orElse("");
        if (!displayPlace) {
            return Pair.of(displayName, dateStr);
        }
        String countryStr = person
                .getPlaceOfBirth()
                .map(Place::country)
                .map(PlaceUtils::adjustCountryForReport)
                .map(country -> country.substring(0, 3))
                .orElseThrow();
        return Pair.of(displayName, dateStr + ", " + countryStr);
    }

    private List<EnrichedPerson> searchPersonByNameAndYear(
            @Nullable SearchPersonDto searchPerson,
            @Nullable String personSurname,
            EnrichedGedcom gedcom) {

        if (searchPerson == null || personSurname == null) {
            return List.of();
        }

        return searchService.findPersonsByNameAndYearOfBirth(
                searchPerson.getGivenName(),
                personSurname,
                searchPerson.getSex(),
                searchPerson.getYearOfBirth(),
                gedcom);
    }

    @Transactional(readOnly = true)
    public List<SearchConnectionDetailsDto> getSearchConnections(
            @Nullable Boolean isMatch,
            @Nullable Boolean isReviewed,
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

        return searchConnectionRepository
                .findAll(Specification
                        .where(SearchConnectionRepository.isMatch(isMatch))
                        .and(SearchConnectionRepository.isReviewed(isReviewed))
                        .and(SearchConnectionRepository.hasContact(hasContact)), pageable)
                .stream()
                .map(searchConnectionMapper::toSearchConnectionDetailsDto)
                .peek(details -> {
                    //noinspection DataFlowIssue
                    if (context != null && !Boolean.TRUE.equals(details.getIsReviewed())) {
                        details.setMarkReviewedLink(context + "/api/search/connection/" + details.getId() + "/reviewed");
                    }
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchConnectionDetailsDto> getLatest(
            @Nullable Boolean isMatch,
            @Nullable Boolean isReviewed,
            @Nullable Boolean hasContact,
            int page,
            int size,
            @Nullable String context) {
        return getSearchConnections(
                isMatch,
                isReviewed,
                hasContact,
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id"),
                context);
    }

}
