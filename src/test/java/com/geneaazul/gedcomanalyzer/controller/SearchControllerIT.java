package com.geneaazul.gedcomanalyzer.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchControllerIT extends AbstractControllerIT {

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
                        .placeOfBirth("Azul")
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
                        .placeOfBirth("Tapalqué")
                        .build())
                .contact("juan.perez@gmail.com")
                .build();

        doReturn(SearchFamily.builder()
                .id(1L)
                .build())
                .when(searchFamilyRepository)
                .save(any());

        String url = "/api/search/family";
        MvcResult result = mvc.perform(post(url)
                        .content(objectMapper.writeValueAsBytes(searchFamilyDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.people", hasSize(2)))
                .andReturn();

        log.info(url + " response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testSearchFamilyLatest() throws Exception {

        doReturn(new PageImpl<>(
                List.of(
                        SearchFamily.builder()
                                .id(1L)
                                .isMatch(true)
                                .build()
                )))
                .when(searchFamilyRepository)
                .findAll(any(Pageable.class));

        String url = "/api/search/family/latest?page=0&size=5";
        MvcResult result = mvc.perform(get(url)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();

        log.info(url + " response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testSearchSurnames() throws Exception {
        ObjectMapper objectMapper = mapperBuilder.build();

        SearchSurnamesDto searchSurnamesDto = SearchSurnamesDto.builder()
                .surnames(List.of(
                        "Family1",
                        "Family2",
                        "family1",
                        "Other Surname"))
                .build();

        String url = "/api/search/surnames";
        MvcResult result = mvc.perform(post(url)
                        .content(objectMapper.writeValueAsBytes(searchSurnamesDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surnames", hasSize(3)))
                .andReturn();

        log.info(url + " response: {}", result.getResponse().getContentAsString());
    }

}
