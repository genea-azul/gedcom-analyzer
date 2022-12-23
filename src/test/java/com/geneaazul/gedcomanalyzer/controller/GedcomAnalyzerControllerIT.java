package com.geneaazul.gedcomanalyzer.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.PersonMapper;
import com.geneaazul.gedcomanalyzer.service.GedcomAnalyzerService;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;
import com.geneaazul.gedcomanalyzer.service.SearchService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URL;

@WebMvcTest(GedcomAnalyzerController.class)
@Import({GedcomParsingService.class, GedcomAnalyzerService.class, GedcomAnalyzerProperties.class, SearchService.class, PersonMapper.class})
class GedcomAnalyzerControllerIT {

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

        mvc.perform(multipart(HttpMethod.POST, "/analyzer")
                        .file(multipartFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personsCount", is(3)))
                .andExpect(jsonPath("$.familiesCount", is(1)))
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

        mvc.perform(multipart(HttpMethod.POST, "/analyzer")
                        .file(multipartFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personsCount", is(3)))
                .andExpect(jsonPath("$.familiesCount", is(1)))
                .andExpect(jsonPath("$.personDuplicates", hasSize(0)))
                .andExpect(jsonPath("$.invalidAlivePersons", hasSize(0)));
    }

}
