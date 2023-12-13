package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.PlaceUtils;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PlainFamilyTreeTxtService extends PlainFamilyTreeService {

    private final RelationshipMapper relationshipMapper;

    public PlainFamilyTreeTxtService(
            GedcomHolder gedcomHolder,
            FamilyTreeHelper familyTreeHelper,
            GedcomAnalyzerProperties properties,
            RelationshipMapper relationshipMapper) {
        super(
                gedcomHolder,
                familyTreeHelper,
                properties,
                "txt",
                MediaType.TEXT_PLAIN);
        this.relationshipMapper = relationshipMapper;
    }

    @Override
    protected void export(
            Path exportFilePath,
            EnrichedPerson person,
            boolean obfuscateLiving,
            List<List<Relationship>> peopleInTree) {
        log.info("Generating Plain family tree TXT");

        List<FormattedRelationship> formattedRelationships = peopleInTree
                .stream()
                .map(relationships -> relationships
                        .stream()
                        .map(relationship -> relationshipMapper.toRelationshipDto(relationship, obfuscateLiving))
                        .map(relationship -> relationshipMapper.formatInSpanish(relationship, false))
                        .toList())
                .map(frs -> frs
                        .stream()
                        .reduce(FormattedRelationship::mergeRelationshipDesc)
                        .orElseThrow())
                .toList();

        try {
            exportToTXT(exportFilePath, person, formattedRelationships);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void exportToTXT(
            Path exportFilePath,
            EnrichedPerson person,
            List<FormattedRelationship> peopleInTree) throws IOException {

        Stream<String> header = Stream.of(
                "Árbol genealógico de " + person.getDisplayName(),
                "",
                "Personas:  " + person.getPersonsCountInTree(),
                "Apellidos (en caso de apellidos compuestos sólo se considera el primero):  " + person.getSurnamesCountInTree(),
                "Generaciones:  " + person.getAncestryGenerations().getTotalGenerations()
                        + "  (ascendencia directa: " + person.getAncestryGenerations().ascending()
                        + ", descendencia directa: " + person.getAncestryGenerations().directDescending() + ")",
                "Países en su ascendencia:  " + (person.getAncestryCountries().isEmpty() ? "-" : String.join(", ", person.getAncestryCountries())),
                "");

        Stream<String> people = peopleInTree
                .stream()
                .map(fr -> String.format("%4s. %1s %1s %1s %-3.3s %5s  %-50.50s %1s • %s%s",
                        StringUtils.defaultString(fr.index()),
                        StringUtils.defaultString(fr.personSex()),
                        StringUtils.defaultString(fr.treeSide()),
                        StringUtils.defaultString(fr.personIsAlive()),
                        StringUtils.defaultString(PlaceUtils.adjustCountryForReport(fr.personCountryOfBirth())),
                        StringUtils.defaultString(fr.personYearOfBirth()),
                        StringUtils.defaultString(fr.personName()),
                        StringUtils.defaultString(fr.distinguishedPerson()),
                        StringUtils.defaultString(fr.relationshipDesc()),
                        fr.adoption() != null ? "  [rama adoptiva]" : ""));

        List<String> lines = Stream.concat(header, people).toList();
        Files.write(exportFilePath, lines, StandardCharsets.UTF_8);
    }

}
