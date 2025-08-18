package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.Application;
import com.geneaazul.gedcomanalyzer.repository.SearchConnectionRepository;
import com.geneaazul.gedcomanalyzer.repository.SearchFamilyRepository;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest({SearchController.class, GedcomAnalyzerController.class})
@ComponentScan(basePackageClasses = Application.class)
@ActiveProfiles("test")
public abstract class AbstractControllerIT {

    @MockitoBean
    protected SearchFamilyRepository searchFamilyRepository;
    @MockitoBean
    protected SearchConnectionRepository searchConnectionRepository;

}
