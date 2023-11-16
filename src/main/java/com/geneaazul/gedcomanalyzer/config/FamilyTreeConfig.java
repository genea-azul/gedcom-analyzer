package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.service.familytree.FamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.familytree.NetworkFamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.familytree.PlainFamilyTreeService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class FamilyTreeConfig {

    private final GedcomAnalyzerProperties properties;
    private final ExecutorService singleThreadExecutorService;

    @Bean
    public Map<FamilyTreeType, FamilyTreeService> familyTreeServiceByType(
            PlainFamilyTreeService plainFamilyTreeService,
            NetworkFamilyTreeService networkFamilyTreeService) {
        return Map.of(
                FamilyTreeType.PLAIN, plainFamilyTreeService,
                FamilyTreeType.NETWORK, networkFamilyTreeService);
    }

    @PostConstruct
    public void extractPyvisNetworkScript() {
        singleThreadExecutorService.submit(() -> {
            try {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource resource = resolver.getResource(properties.getPyvisNetworkExportScriptPath());
                Files.createDirectories(properties.getTempDir());
                Path newScriptPath = properties.getTempDir().resolve(properties.getPyvisNetworkExportScriptPath());
                Files.copy(resource.getInputStream(), newScriptPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

}
