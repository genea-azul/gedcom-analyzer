package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Server-side privacy and abuse limits: obfuscation can't be turned off by the client,
 * tree exports are rate-limited per client, and rejected searches are not stored.
 */
@TestPropertySource(properties = {
        "disable-obfuscate-living=false",
        "max-client-family-tree-requests-count-threshold=2"
})
class SearchControllerPolicyIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID searchPersonUuid(String clientIp) throws Exception {
        SearchFamilyDto searchFamilyDto = SearchFamilyDto.builder()
                .individual(SearchPersonDto.builder()
                        .givenName("Son")
                        .surname("B&A")
                        .yearOfBirth(2000)
                        .build())
                .father(SearchPersonDto.builder()
                        .givenName("Test Father")
                        .sex(SexType.M)
                        .build())
                .build();
        doReturn(SearchFamily.builder().id(1L).build()).when(searchFamilyRepository).save(any());

        String response = mvc.perform(post("/api/search/family")
                        .header("Fly-Client-IP", clientIp)
                        .content(objectMapper.writeValueAsBytes(searchFamilyDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, SearchFamilyResultDto.class).getPeople().getFirst().getUuid();
    }

    @Test
    void graphJsonHidesLivingPeopleEvenWhenTheClientAsksNotTo() throws Exception {
        UUID personUuid = searchPersonUuid("203.0.113.10");

        String json = mvc.perform(get("/api/search/family-tree/" + personUuid + "/graphJson")
                        .header("Fly-Client-IP", "203.0.113.10")
                        .queryParam("obfuscateLiving", "false"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<JsonNode> alive = StreamSupport.stream(objectMapper.readTree(json).get("persons").spliterator(), false)
                .filter(node -> node.path("isAlive").asBoolean(false))
                .toList();
        assertThat(alive).isNotEmpty();
        assertThat(alive).allSatisfy(node -> {
            assertThat(node.get("displayName").asString()).startsWith(PersonUtils.PRIVATE_NAME);
            assertThat(node.path("aka").isMissingNode() || node.path("aka").isNull()).isTrue();
            assertThat(node.path("profilePicture").isMissingNode() || node.path("profilePicture").isNull()).isTrue();
            assertThat(node.path("yearOfBirth").isMissingNode() || node.path("yearOfBirth").isNull()).isTrue();
        });
    }

    @Test
    void treeExportsAreRateLimitedPerClient() throws Exception {
        UUID personUuid = searchPersonUuid("203.0.113.20");
        String url = "/api/search/family-tree/" + personUuid + "/graphJson";

        mvc.perform(get(url).header("Fly-Client-IP", "203.0.113.20")).andExpect(status().isOk());
        mvc.perform(get(url).header("Fly-Client-IP", "203.0.113.20")).andExpect(status().isOk());
        mvc.perform(get(url).header("Fly-Client-IP", "203.0.113.20"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("TOO-MANY-REQUESTS"));

        mvc.perform(get(url).header("Fly-Client-IP", "203.0.113.21")).andExpect(status().isOk());
    }

    @Test
    void rejectedSearchesAreNotStored() throws Exception {
        doReturn(1_000L).when(searchFamilyRepository).countByClientIpAddressAndCreateDateBetween(anyString(), any(), any());
        SearchFamilyDto searchFamilyDto = SearchFamilyDto.builder()
                .individual(SearchPersonDto.builder().givenName("Son").surname("B&A").build())
                .build();

        mvc.perform(post("/api/search/family")
                        .header("Fly-Client-IP", "203.0.113.30")
                        .content(objectMapper.writeValueAsBytes(searchFamilyDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors", contains("TOO-MANY-REQUESTS")));

        verify(searchFamilyRepository, never()).save(any());
    }

}
