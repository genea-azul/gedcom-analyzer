package com.geneaazul.gedcomanalyzer.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geneaazul.gedcomanalyzer.GedcomAnalyzerApplication;
import com.geneaazul.gedcomanalyzer.model.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.model.SexType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
@ComponentScan(basePackageClasses = GedcomAnalyzerApplication.class)
@ActiveProfiles("test")
class SearchControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Jackson2ObjectMapperBuilder mapperBuilder;

    @Test
    public void testSearchFamily() throws Exception {
        ObjectMapper objectMapper = mapperBuilder.build();

        SearchFamilyDto searchFamilyDto = SearchFamilyDto.builder()
                .individual(SearchPersonDto.builder()
                        .givenName("Some")
                        .surname("Person")
                        .sex(SexType.M)
                        .isAlive(Boolean.TRUE)
                        .yearOfBirth(2020)
                        .build())
                .maternalGrandfather(SearchPersonDto.builder()
                        .givenName("Father")
                        .surname("Family1")
                        .sex(SexType.M)
                        .isAlive(Boolean.FALSE)
                        .yearOfBirth(1980)
                        .build())
                .maternalGrandmother(SearchPersonDto.builder()
                        .givenName("Mother")
                        .surname("Family2")
                        .sex(SexType.F)
                        .isAlive(Boolean.FALSE)
                        .yearOfBirth(1985)
                        .build())
                .contact("juan.perez@gmail.com")
                .build();

        System.out.println(objectMapper.writeValueAsString(searchFamilyDto));

        mvc.perform(post("/api/search/family")
                        .content(objectMapper.writeValueAsBytes(searchFamilyDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.people", hasSize(2)));
    }

}
