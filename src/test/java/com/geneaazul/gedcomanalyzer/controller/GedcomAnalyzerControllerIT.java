package com.geneaazul.gedcomanalyzer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

}
