package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class PlainFamilyTreeService implements FamilyTreeService {

    protected final GedcomHolder gedcomHolder;
    protected final FamilyTreeHelper familyTreeHelper;
    protected final GedcomAnalyzerProperties properties;

    @Getter
    protected final String exportFileExtension;
    @Getter
    protected final MediaType exportFileMediaType;

    protected abstract void export(
            Path exportFilePath,
            EnrichedPerson person,
            boolean obfuscateLiving,
            List<List<Relationship>> relationshipsWithNotInLawPriority);

    @Override
    public boolean isMissingFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix,
            boolean obfuscateLiving) {

        Path pdfExportFilePath = getExportFilePath(
                person,
                familyTreeFileIdPrefix,
                familyTreeFileSuffix);

        return Files.notExists(pdfExportFilePath);
    }

    @Override
    public void generateFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix,
            boolean obfuscateLiving,
            List<List<Relationship>> relationshipsWithNotInLawPriority) {

        Path exportFilePath = getExportFilePath(
                person,
                familyTreeFileIdPrefix,
                familyTreeFileSuffix);

        export(
                exportFilePath,
                person,
                obfuscateLiving,
                relationshipsWithNotInLawPriority);
    }

    @Override
    public Optional<FamilyTree> getFamilyTree(
            UUID personUuid,
            boolean obfuscateLiving,
            boolean forceRewrite) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = gedcom.getPersonByUuid(personUuid);
        if (person == null) {
            return Optional.empty();
        }

        String familyTreeFileIdPrefix = familyTreeHelper.getFamilyTreeFileId(person);
        String familyTreeFileSuffix = obfuscateLiving ? "" : "_visible";

        Path exportFilePath = getExportFilePath(
                person,
                familyTreeFileIdPrefix,
                familyTreeFileSuffix);

        if (forceRewrite || Files.notExists(exportFilePath)) {
            List<List<Relationship>> relationshipsWithNotInLawPriority = familyTreeHelper
                    .getRelationshipsWithNotInLawPriority(person);

            generateFamilyTree(
                    person,
                    familyTreeFileIdPrefix,
                    familyTreeFileSuffix,
                    obfuscateLiving,
                    relationshipsWithNotInLawPriority);
        }

        return Optional.of(new FamilyTree(
                person,
                "genea_azul_arbol_" + familyTreeFileIdPrefix + "." + exportFileExtension,
                exportFilePath,
                exportFileMediaType,
                properties.getLocale()));
    }

    private Path getExportFilePath(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix) {

        return properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(familyTreeFileIdPrefix + "_" + person.getUuid() + familyTreeFileSuffix + "." + exportFileExtension);
    }

}
