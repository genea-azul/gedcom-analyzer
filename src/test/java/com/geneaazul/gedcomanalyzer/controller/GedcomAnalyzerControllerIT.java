package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.repository.projection.SearchConnectionProjection;
import com.geneaazul.gedcomanalyzer.repository.projection.SearchFamilyProjection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class GedcomAnalyzerControllerIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mvc;

    @Test
    public void testAnalyzeGedcom() throws Exception {
        URL gedcomFile = getClass().getClassLoader().getResource("gedcom/test-gedcom-001.ged");
        assert gedcomFile != null;

        MockMultipartFile multipartFile = new MockMultipartFile(
                "gedcomFile",
                "test-gedcom-001.ged",
                "plain/text",
                gedcomFile.openStream());

        mvc.perform(multipart(HttpMethod.POST, "/api/gedcom-analyzer")
                        .file(multipartFile)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personsCount", is(22)))
                .andExpect(jsonPath("$.personDuplicates", hasSize(0)))
                .andExpect(jsonPath("$.invalidAlivePersons", hasSize(0)));
    }

    @Test
    public void testAnalyzeZipGedcom() throws Exception {
        URL gedcomFile = getClass().getClassLoader().getResource("gedcom/test-gedcom-001.ged.zip");
        assert gedcomFile != null;

        MockMultipartFile multipartFile = new MockMultipartFile(
                "gedcomFile",
                "test-gedcom-001.ged.zip",
                "application/zip",
                gedcomFile.openStream());

        mvc.perform(multipart(HttpMethod.POST, "/api/gedcom-analyzer")
                        .file(multipartFile)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personsCount", is(5)))
                .andExpect(jsonPath("$.personDuplicates", hasSize(0)))
                .andExpect(jsonPath("$.invalidAlivePersons", hasSize(0)));
    }

    @Test
    public void testUsageStats() throws Exception {
        String url = "/api/gedcom-analyzer/usageStats";
        MvcResult result = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientsCount", is(0)))
                .andExpect(jsonPath("$.searchesCount", is(0)))
                .andExpect(jsonPath("$.clientStats", hasSize(0)))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        doReturn(List.<SearchFamilyProjection>of(
                new SearchFamilyProjection() {
                    @Override
                    public String getClientIpAddress() {
                        return "the-client-ip";
                    }

                    @Override
                    public Long getCount() {
                        return 2L;
                    }

                    @Override
                    public OffsetDateTime getMinCreateDate() {
                        return null;
                    }

                    @Override
                    public OffsetDateTime getMaxCreateDate() {
                        return null;
                    }

                    @Override
                    public List<String> getIndividualSurnames() {
                        return List.of("surname-1", "surname-2");
                    }
                }
        ))
                .when(searchFamilyRepository)
                .groupByClientIpAddress();

        doReturn(List.<SearchConnectionProjection>of(
                new SearchConnectionProjection() {
                    @Override
                    public String getClientIpAddress() {
                        return "the-client-ip";
                    }

                    @Override
                    public Long getCount() {
                        return 3L;
                    }

                    @Override
                    public OffsetDateTime getMinCreateDate() {
                        return null;
                    }

                    @Override
                    public OffsetDateTime getMaxCreateDate() {
                        return null;
                    }

                    @Override
                    public List<String> getPerson1Surnames() {
                        return List.of("surname-1", "surname-2");
                    }

                    @Override
                    public List<String> getPerson2Surnames() {
                        return List.of("surname-2", "surname-3");
                    }
                }
        ))
                .when(searchConnectionRepository)
                .groupByClientIpAddress();

        url = "/api/gedcom-analyzer/usageStats";
        result = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientsCount", is(1)))
                .andExpect(jsonPath("$.searchesCount", is(5)))
                .andExpect(jsonPath("$.clientStats", hasSize(1)))
                .andExpect(jsonPath("$.clientStats[0].ipAddress", is("the-client-ip")))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

}
