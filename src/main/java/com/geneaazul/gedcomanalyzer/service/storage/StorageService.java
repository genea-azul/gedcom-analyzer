package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

import java.time.Instant;
import java.util.Optional;

public interface StorageService {

    EnrichedGedcom getGedcom(boolean refreshCachedGedcom) throws Exception;

    String getGedcomName();

    /**
     * The modification time of the source's GEDCOM when it is newer than the cached copy; empty when it
     * isn't, or for sources without change detection.
     */
    default Optional<Instant> findNewerGedcomVersion() throws Exception {
        return Optional.empty();
    }

}
