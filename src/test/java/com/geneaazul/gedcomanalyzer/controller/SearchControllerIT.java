package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class SearchControllerIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Jackson2ObjectMapperBuilder mapperBuilder;

    @Value("${test.individual.givenName:Test Son}")
    private String individualGivenName;
    @Value("${test.individual.surname:Biological and Adoptive}")
    private String individualSurname;
    @Value("${test.individual.yearOfBirth:2000}")
    private Integer individualYearOfBirth;
    @Value("${test.spouse.givenName:}")
    private String spouseGivenName;
    @Value("${test.spouse.surname:}")
    private String spouseSurname;
    @Value("${test.father.givenName:Test Father}")
    private String fatherGivenName;

    @Test
    public void testSearchFamily() throws Exception {
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
        MvcResult result = mvc.perform(post(url)
                        .content(objectMapper.writeValueAsBytes(searchFamilyDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.people", hasSize(2)))
                .andReturn();

        log.info(url + " response: {}", result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSearchFamilyLatestToReview() throws Exception {

        doReturn(new PageImpl<>(
                List.of(
                        SearchFamily.builder()
                                .id(1L)
                                .isMatch(false)
                                .isReviewed(null)
                                .contact("@contact")
                                .build()
                )))
                .when(searchFamilyRepository)
                .findAll(any(Specification.class), any(Pageable.class));

        String url = "/api/search/family/latest?isReviewed=false&page=0&size=5";
        MvcResult result = mvc.perform(get(url)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();

        log.info(url + " response: {}", result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testMarkFamilyReviewed() throws Exception {

        doReturn(Optional.of(
                SearchFamily.builder()
                        .id(1L)
                        .isMatch(false)
                        .isReviewed(null)
                        .build()
        ))
                .when(searchFamilyRepository)
                .findById(1L);

        String url = "/api/search/family/1/reviewed";
        MvcResult result = mvc.perform(get(url)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isReviewed", is(true)))
                .andReturn();

        log.info(url + " response: {}", result.getResponse().getContentAsString(StandardCharsets.UTF_8));
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

        log.info(url + " response: {}", result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testSearchFamilyTree() throws Exception {
        ObjectMapper objectMapper = mapperBuilder.build();

        SearchFamilyDto searchFamilyDto = SearchFamilyDto.builder()
                .individual(SearchPersonDto.builder()
                        .givenName(individualGivenName)
                        .surname(individualSurname)
                        .yearOfBirth(individualYearOfBirth)
                        .build())
                .spouse(SearchPersonDto.builder()
                        .givenName(spouseGivenName)
                        .surname(spouseSurname)
                        .build())
                .father(SearchPersonDto.builder()
                        .givenName(fatherGivenName)
                        .sex(SexType.M)
                        .build())
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
                .andExpect(jsonPath("$.people", hasSize(StringUtils.isEmpty(spouseGivenName) ? 1 : 2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        SearchFamilyResultDto searchFamilyResult = objectMapper.readValue(searchResult, SearchFamilyResultDto.class);

        UUID personUuid = searchFamilyResult.getPeople().get(0).getUuid();

        url = "/api/search/family-tree/" + personUuid + "/plain";
        MvcResult result = mvc.perform(get(url)
                        .queryParam("obfuscateLiving", "false")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        log.info(url + " response: {}", result.getResponse().getContentAsString(StandardCharsets.ISO_8859_1).substring(0, 50));
    }

}
