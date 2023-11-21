package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class AppControllerIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Jackson2ObjectMapperBuilder mapperBuilder;

    @Test
    public void testDisplayFamilyTreeNetwork() throws Exception {
        ObjectMapper objectMapper = mapperBuilder.build();

        SearchFamilyDto searchFamilyDto = SearchFamilyDto.builder()
                .individual(SearchPersonDto.builder()
                        .givenName("Some")
                        .surname("Person")
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
                .obfuscateLiving(false)
                .build();

        doReturn(SearchFamily.builder()
                .id(1L)
                .build())
                .when(searchFamilyRepository)
                .save(any());

        String url = "/api/search/family";
        String searchResult = mvc.perform(post(url)
                        .content(objectMapper.writeValueAsBytes(searchFamilyDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.people", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        SearchFamilyResultDto searchFamilyResult = objectMapper.readValue(searchResult, SearchFamilyResultDto.class);

        UUID personUuid = searchFamilyResult.getPeople().get(0).getUuid();

        url = "/family-tree/" + personUuid;
        MvcResult result = mvc.perform(get(url)
                        .queryParam("f", "0")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        log.info(url + " response:\n{}", result.getResponse().getContentAsString(StandardCharsets.ISO_8859_1).substring(0, 50));

        url = "/family-tree/" + personUuid + "/network";
        result = mvc.perform(get(url)
                        .queryParam("f", "0")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        log.info(url + " response:\n{}", result.getResponse().getContentAsString(StandardCharsets.ISO_8859_1).substring(0, 50));
    }

}
