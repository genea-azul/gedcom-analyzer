package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final GedcomParsingService gedcomParsingService;
    private final GedcomAnalyzerProperties properties;

    public EnrichedGedcom getGedcom() throws Exception {
        return gedcomParsingService.parse(properties.getLocalStorageGedcomPath());
    }

    @Override
    public EnrichedGedcom getGedcom(boolean refreshCachedGedcom) throws Exception {
        return getGedcom();
    }

    @Override
    public String getGedcomName() {
        return properties.getLocalStorageGedcomPath().toString();
    }

}
