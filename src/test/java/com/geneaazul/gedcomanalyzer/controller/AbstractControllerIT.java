package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.GedcomAnalyzerApplication;
import com.geneaazul.gedcomanalyzer.repository.SearchConnectionRepository;
import com.geneaazul.gedcomanalyzer.repository.SearchFamilyRepository;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@WebMvcTest({SearchController.class, GedcomAnalyzerController.class})
@ComponentScan(basePackageClasses = GedcomAnalyzerApplication.class)
@ActiveProfiles("test")
public abstract class AbstractControllerIT {

    @MockBean
    protected SearchFamilyRepository searchFamilyRepository;
    @MockBean
    protected SearchConnectionRepository searchConnectionRepository;

}
