package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.service.familytree.FamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.familytree.GraphJsonFamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.familytree.PlainFamilyTreePdfService;
import com.geneaazul.gedcomanalyzer.service.familytree.PlainFamilyTreeTxtService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class FamilyTreeConfig {

    @Bean
    public Map<FamilyTreeType, FamilyTreeService> familyTreeServiceByType(
            PlainFamilyTreePdfService plainFamilyTreePdfService,
            PlainFamilyTreeTxtService plainFamilyTreeTxtService,
            GraphJsonFamilyTreeService graphJsonFamilyTreeService) {
        return Map.of(
                FamilyTreeType.PLAIN_PDF, plainFamilyTreePdfService,
                FamilyTreeType.PLAIN_TXT, plainFamilyTreeTxtService,
                FamilyTreeType.GRAPH_JSON, graphJsonFamilyTreeService);
    }

}
