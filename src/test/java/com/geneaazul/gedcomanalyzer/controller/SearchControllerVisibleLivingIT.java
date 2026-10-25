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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With obfuscation disabled on the server (production setting), graph JSON shows living people in full.
 */
@TestPropertySource(properties = "disable-obfuscate-living=true")
class SearchControllerVisibleLivingIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID searchPersonUuid(String clientIp) throws Exception {
        // Test Father /Family1/ and his family were born in Azul
        SearchFamilyDto searchFamilyDto = SearchFamilyDto.builder()
                .individual(SearchPersonDto.builder()
                        .givenName("Test Father")
                        .surname("Family1")
                        .sex(SexType.M)
                        .yearOfBirth(1980)
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
    void graphJsonShowsNamesAndBirthplaces() throws Exception {
        UUID personUuid = searchPersonUuid("203.0.113.40");

        String json = mvc.perform(get("/api/search/family-tree/" + personUuid + "/graphJson")
                        .header("Fly-Client-IP", "203.0.113.40"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<JsonNode> persons = StreamSupport.stream(objectMapper.readTree(json).get("persons").spliterator(), false).toList();
        assertThat(persons).noneSatisfy(node -> assertThat(node.get("displayName").asString()).startsWith(PersonUtils.PRIVATE_NAME));
        assertThat(persons).anySatisfy(node -> assertThat(node.path("placeOfBirth").asString()).contains("Azul"));
    }

}
