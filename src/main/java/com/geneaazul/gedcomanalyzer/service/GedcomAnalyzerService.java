package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.DateTimeMapper;
import com.geneaazul.gedcomanalyzer.mapper.GedcomMapper;
import com.geneaazul.gedcomanalyzer.mapper.ObfuscationType;
import com.geneaazul.gedcomanalyzer.mapper.PersonMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.dto.ClientStatsDto;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomAnalysisDto;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomMetadataDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateDto;
import com.geneaazul.gedcomanalyzer.model.dto.UsageStatsDto;
import com.geneaazul.gedcomanalyzer.repository.SearchConnectionRepository;
import com.geneaazul.gedcomanalyzer.repository.SearchFamilyRepository;
import com.geneaazul.gedcomanalyzer.repository.projection.SearchConnectionProjection;
import com.geneaazul.gedcomanalyzer.repository.projection.SearchFamilyProjection;
import com.geneaazul.gedcomanalyzer.utils.MapUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.folg.gedcom.model.Gedcom;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GedcomAnalyzerService {

    private final SearchService searchService;
    private final SearchFamilyRepository searchFamilyRepository;
    private final SearchConnectionRepository searchConnectionRepository;
    private final PersonMapper personMapper;
    private final GedcomMapper gedcomMapper;
    private final DateTimeMapper dateTimeMapper;

    public GedcomMetadataDto getGedcomMetadata(EnrichedGedcom gedcom) {
        return getGedcomMetadata(gedcom, null);
    }

    public GedcomMetadataDto getGedcomMetadata(EnrichedGedcom gedcom, @Nullable Duration reloadDuration) {
        return gedcomMapper.toGedcomMetadataDto(gedcom, reloadDuration);
    }

    public GedcomAnalysisDto analyze(EnrichedGedcom gedcom) {
        log.info("Analyze gedcom: {}", gedcom.getGedcomName());

        List<PersonComparisonResults> duplicatedPersons = searchService.findDuplicatedPersons(gedcom);
        List<PersonDuplicateDto> personDuplicatesDto = personMapper.toPersonDuplicateDto(duplicatedPersons, ObfuscationType.NONE);

        List<EnrichedPerson> alivePersons = searchService.findAlivePersonsTooOldOrWithFamilyMembersTooOld(gedcom.getPeople());
        List<PersonDto> invalidAlivePersonsDto = personMapper.toPersonDto(alivePersons, ObfuscationType.NONE);

        log.info("Gedcom analyzed: {}", gedcom.getGedcomName());
        return GedcomAnalysisDto.builder()
                .personsCount(gedcom.getPeople().size())
                .personDuplicates(personDuplicatesDto)
                .invalidAlivePersons(invalidAlivePersonsDto)
                .build();
    }

    @Transactional(readOnly = true)
    public UsageStatsDto getUsageStats() {
        List<SearchFamilyProjection> searchFamilyProjections = searchFamilyRepository.groupByClientIpAddress();
        List<SearchConnectionProjection> searchConnectionProjections = searchConnectionRepository.groupByClientIpAddress();

        List<ClientStatsDto> clientStats = Stream
                .concat(
                        searchFamilyProjections
                                .stream()
                                .map(proj -> ClientStatsDto.builder()
                                        .ipAddress(proj.getClientIpAddress())
                                        .searchesCount(Math.toIntExact(proj.getCount()))
                                        .firstSearch(dateTimeMapper.toLocalDate(proj.getMinCreateDate()))
                                        .lastSearch(dateTimeMapper.toLocalDate(proj.getMaxCreateDate()))
                                        .topSurnames(ListUtils.emptyIfNull(proj.getIndividualSurnames()).stream().filter(StringUtils::isNotEmpty).toList())
                                        .build()),
                        searchConnectionProjections
                                .stream()
                                .map(proj -> ClientStatsDto.builder()
                                        .ipAddress(proj.getClientIpAddress())
                                        .searchesCount(Math.toIntExact(proj.getCount()))
                                        .firstSearch(dateTimeMapper.toLocalDate(proj.getMinCreateDate()))
                                        .lastSearch(dateTimeMapper.toLocalDate(proj.getMaxCreateDate()))
                                        .topSurnames(ListUtils.union(
                                                ListUtils.emptyIfNull(proj.getPerson1Surnames()).stream().filter(StringUtils::isNotEmpty).toList(),
                                                ListUtils.emptyIfNull(proj.getPerson2Surnames()).stream().filter(StringUtils::isNotEmpty).toList()))
                                        .build()))
                .filter(cs -> cs.getIpAddress() != null)
                .collect(Collectors.toUnmodifiableMap(
                        ClientStatsDto::getIpAddress,
                        Function.identity(),
                        (cs1, cs2) -> ClientStatsDto.builder()
                                .ipAddress(cs1.getIpAddress())
                                .searchesCount(cs1.getSearchesCount() + cs2.getSearchesCount())
                                .firstSearch(ObjectUtils.min(cs1.getFirstSearch(), cs2.getFirstSearch()))
                                .lastSearch(ObjectUtils.max(cs1.getLastSearch(), cs2.getLastSearch()))
                                .topSurnames(ListUtils.union(cs1.getTopSurnames(), cs2.getTopSurnames()))
                                .build()))
                .values()
                .stream()
                .sorted(Comparator.comparing(ClientStatsDto::getSearchesCount).reversed())
                .peek(cs -> {
                    log.debug("Top surnames: {}", cs.getTopSurnames());
                    List<String> topSurnames = MapUtils
                            .orderHierarchically(cs.getTopSurnames(), Function.identity(), null)
                            .stream()
                            .map(Triple::getLeft)
                            .toList();
                    cs.setTopSurnames(topSurnames);
                })
                .toList();

        Integer searchesCount = clientStats
                .stream()
                .mapToInt(ClientStatsDto::getSearchesCount)
                .sum();

        return UsageStatsDto.builder()
                .clientsCount(clientStats.size())
                .searchesCount(searchesCount)
                .clientStats(clientStats)
                .build();
    }

}
