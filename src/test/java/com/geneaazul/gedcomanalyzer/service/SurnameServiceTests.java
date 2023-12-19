package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnameResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesResultDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class SurnameServiceTests {

    @Autowired
    private SurnameService surnameService;

    @Test
    public void testSearch() {

        SearchSurnamesDto searchSurnames = SearchSurnamesDto.builder()
                .surnames(List.of(
                        "burgos",
                        "garcia",
                        "perez",
                        "fernandez",
                        "gomez",
                        "perez de villarreal",
                        "de la canal",
                        "b. y miñana gonzalez",
                        "de la rosa fernández",
                        "family1"))
                .build();

        SearchSurnamesResultDto searchSurnamesResult = surnameService.search(searchSurnames);

        assertThat(searchSurnamesResult.getSurnames())
                .hasSize(10);

        searchSurnamesResult.getSurnames()
                .stream()
                .map(SearchSurnameResultDto::getVariants)
                .flatMap(List::stream)
                .distinct()
                .forEach(variant -> assertThat(variant).doesNotEndWithIgnoringCase(" y"));
    }

}
