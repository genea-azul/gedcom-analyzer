package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

public interface StorageService {

    EnrichedGedcom getGedcom(boolean refreshCachedGedcom) throws Exception;

    String getGedcomName();

}
