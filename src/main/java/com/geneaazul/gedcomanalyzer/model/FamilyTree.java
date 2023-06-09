package com.geneaazul.gedcomanalyzer.model;

import org.springframework.http.MediaType;

import java.nio.file.Path;
import java.util.Locale;

public record FamilyTree(EnrichedPerson person, String filename, Path path, MediaType mediaType, Locale locale) {

}
