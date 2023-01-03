package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

public interface StorageService {

    EnrichedGedcom getGedcom() throws Exception;

    String getGedcomName();

}
