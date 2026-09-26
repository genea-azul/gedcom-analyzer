package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.domain.SearchConnection;
import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.model.dto.SearchConnectionDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class SearchControllerIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${test.individual.givenName:Son}")
    private String individualGivenName;
    @Value("${test.individual.surname:B&A}")
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
                .onlySecondaryDescription(true)
                .isForceRewrite(true)
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

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testSearchSurnames() throws Exception {
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

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testSearchFamilyTree() throws Exception {
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
                .obfuscateLiving(false)         // default: true
                .onlySecondaryDescription(true) // default: true
                .isForceRewrite(true)           // default: false
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

        UUID personUuid = searchFamilyResult.getPeople().getFirst().getUuid();

        /*
         * Test download plain family tree PDF
         */
        url = "/api/search/family-tree/" + personUuid + "/plainPdf";
        MvcResult result = mvc.perform(get(url)
                        .queryParam("obfuscateLiving", "false")
                        .queryParam("onlySecondaryDescription", "false") // not considered if not re-generated
                        .queryParam("forceRewrite", "false") // already forced in previous search
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"genea_azul_arbol_son_b&a.pdf\""))
                .andReturn();

        log.info(url + " response:\n{}", result.getResponse().getContentAsString(StandardCharsets.ISO_8859_1).substring(0, 50));

        /*
         * Test view network family tree HTML
         */
        url = "/family-tree/" + personUuid + "/network";
        result = mvc.perform(get(url)
                        .queryParam("f", "0")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.ISO_8859_1).substring(0, 50));
    }

    @Test
    public void testSearchConnection() throws Exception {
        SearchConnectionDto searchConnectionDto = SearchConnectionDto.builder()
                .person1(SearchPersonDto.builder()
                        .givenName("Grandson")
                        .surname("Family1")
                        .yearOfBirth(2022)
                        .build())
                .person2(SearchPersonDto.builder()
                        .givenName("Father")
                        .surname("Family1")
                        .yearOfBirth(1980)
                        .build())
                .build();

        doReturn(SearchConnection.builder()
                .id(1L)
                .build())
                .when(searchConnectionRepository)
                .save(any());

        String url = "/api/search/connection";
        MvcResult result = mvc.perform(post(url)
                        .content(objectMapper.writeValueAsBytes(searchConnectionDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connections", hasSize(3)))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }


    @Autowired
    private GedcomAnalyzerProperties properties;

    @Test
    public void testSearchSurnamesRateLimitedReturns429WithErrorCode() throws Exception {
        doReturn(100L)
                .when(searchFamilyRepository)
                .countByClientIpAddressAndCreateDateBetween(anyString(), any(OffsetDateTime.class), any(OffsetDateTime.class));

        mvc.perform(post("/api/search/surnames")
                        .content(objectMapper.writeValueAsBytes(SearchSurnamesDto.builder()
                                .surnames(List.of("Family1"))
                                .build()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "9.9.9.9")
                        .with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode", is("TOO-MANY-REQUESTS")));
    }

    @Test
    public void testNonPersistedSearchesAreRateLimitedInMemory() throws Exception {
        int originalThreshold = properties.getMaxClientLookupRequestsCountThreshold();
        properties.setMaxClientLookupRequestsCountThreshold(2);
        try {
            byte[] body = objectMapper.writeValueAsBytes(SearchFamilyDto.builder()
                    .individual(SearchPersonDto.builder()
                            .givenName("Some")
                            .surname("Person")
                            .build())
                    .persist(false)
                    .build());

            for (int i = 0; i < 2; i++) {
                mvc.perform(post("/api/search/family")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Forwarded-For", "5.6.7.8")
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.errors", not(hasItem("TOO-MANY-REQUESTS"))));
            }

            mvc.perform(post("/api/search/family")
                            .content(body)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "5.6.7.8")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors", hasItem("TOO-MANY-REQUESTS")))
                    .andExpect(jsonPath("$.people", hasSize(0)));

            // Another client is unaffected
            mvc.perform(post("/api/search/family")
                            .content(body)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "5.6.7.9")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors", not(hasItem("TOO-MANY-REQUESTS"))));
        } finally {
            properties.setMaxClientLookupRequestsCountThreshold(originalThreshold);
        }
    }

    @Test
    public void testPersistFalseDoesNotBypassTheDatabaseQuota() throws Exception {
        doReturn(100L)
                .when(searchFamilyRepository)
                .countByClientIpAddressAndCreateDateBetween(anyString(), any(OffsetDateTime.class), any(OffsetDateTime.class));

        mvc.perform(post("/api/search/family")
                        .content(objectMapper.writeValueAsBytes(SearchFamilyDto.builder()
                                .individual(SearchPersonDto.builder()
                                        .givenName("Some")
                                        .surname("Person")
                                        .build())
                                .persist(false)
                                .build()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "7.7.7.7")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors", hasItem("TOO-MANY-REQUESTS")))
                .andExpect(jsonPath("$.people", hasSize(0)));
    }

    @Test
    public void testPersistFalseIsDeniedAtExactlyTheDatabaseQuota() throws Exception {
        // Exactly at the quota: a stored search would be the one over it, and so is a non-stored lookup
        doReturn((long) properties.getMaxClientRequestsCountThreshold())
                .when(searchFamilyRepository)
                .countByClientIpAddressAndCreateDateBetween(anyString(), any(OffsetDateTime.class), any(OffsetDateTime.class));

        mvc.perform(post("/api/search/family")
                        .content(objectMapper.writeValueAsBytes(SearchFamilyDto.builder()
                                .individual(SearchPersonDto.builder()
                                        .givenName("Some")
                                        .surname("Person")
                                        .build())
                                .persist(false)
                                .build()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "7.7.7.8")
                        .with(csrf()))
                .andExpect(jsonPath("$.errors", hasItem("TOO-MANY-REQUESTS")));
    }

    @Test
    public void testSearchesAreLimitedInMemoryWhenStoringIsDisabled() throws Exception {
        int originalThreshold = properties.getMaxClientLookupRequestsCountThreshold();
        properties.setMaxClientLookupRequestsCountThreshold(1);
        properties.setStoreFamilySearch(false);
        try {
            byte[] body = objectMapper.writeValueAsBytes(SearchFamilyDto.builder()
                    .individual(SearchPersonDto.builder()
                            .givenName("Some")
                            .surname("Person")
                            .build())
                    .build());

            mvc.perform(post("/api/search/family")
                            .content(body)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "8.8.4.4")
                            .with(csrf()))
                    .andExpect(jsonPath("$.errors", not(hasItem("TOO-MANY-REQUESTS"))));

            mvc.perform(post("/api/search/family")
                            .content(body)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "8.8.4.4")
                            .with(csrf()))
                    .andExpect(jsonPath("$.errors", hasItem("TOO-MANY-REQUESTS")));
        } finally {
            properties.setMaxClientLookupRequestsCountThreshold(originalThreshold);
            properties.setStoreFamilySearch(true);
        }
    }

    @Test
    public void testSearchFamilyFieldTooLongReturnsInvalidRequestErrorCode() throws Exception {
        mvc.perform(post("/api/search/family")
                        .content(objectMapper.writeValueAsBytes(SearchFamilyDto.builder()
                                .individual(SearchPersonDto.builder()
                                        .givenName("A".repeat(61))
                                        .surname("Person")
                                        .build())
                                .build()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID-REQUEST")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://geneaazul.com.ar",
            "https://www.geneaazul.com.ar",
            "https://geneaazul-web.pages.dev",
            "https://3f2a1b9c.geneaazul-web.pages.dev",
            "https://fix-router.geneaazul-web.pages.dev",
    })
    public void testCorsPreflightAllowsWebsiteOrigins(String origin) throws Exception {
        mvc.perform(options("/api/search/family")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.example.com",
            "https://other-project.pages.dev",
            "https://geneaazul-web.pages.dev.evil.com",
    })
    public void testCorsPreflightRejectsOtherOrigins(String origin) throws Exception {
        mvc.perform(options("/api/search/family")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testCorsExposesDownloadHeadersOnPdfEndpoint() throws Exception {
        mvc.perform(options("/api/search/family-tree/" + UUID.randomUUID() + "/plainPdf")
                        .header(HttpHeaders.ORIGIN, "https://geneaazul.com.ar")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/search/family-tree/" + UUID.randomUUID() + "/plainPdf")
                        .header(HttpHeaders.ORIGIN, "https://geneaazul.com.ar"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("File-Name")));
    }

}
