package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

/**
 * Published by {@link GedcomHolder} every time a GEDCOM becomes the one being served.
 */
public record GedcomLoadedEvent(EnrichedGedcom gedcom) {
}
