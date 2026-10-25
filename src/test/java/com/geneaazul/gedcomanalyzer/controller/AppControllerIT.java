package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class AppControllerIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void legacyPagesRedirectToTheWebsite() throws Exception {
        for (String url : List.of("/", "/no-redirect", "/family-tree/" + UUID.randomUUID(), "/family-tree/" + UUID.randomUUID() + "/network")) {
            mvc.perform(get(url))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string(HttpHeaders.LOCATION, "https://geneaazul.com.ar"));
        }
    }

    @Test
    public void testTreeBuilderLatestPage() throws Exception {
        String url = "/tree-builder/latest";
        MvcResult result = mvc.perform(get(url).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.ISO_8859_1).substring(0, 50));
    }

}
