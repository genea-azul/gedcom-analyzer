package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared skeleton of the family-tree exporters: file naming, lookup, and generation.
 *
 * <p>Generation is serialized per file (the background task queued by a search and a download
 * request often ask for the same file at once) and writes to a temporary sibling that is moved
 * into place atomically, so a reader never gets a half-written file.
 */
@Slf4j
public abstract class AbstractFamilyTreeService implements FamilyTreeService {

    private static final String FAMILY_TREES_DIR = "family-trees";
    private static final String VISIBLE_SUFFIX = "_visible";
    // One small entry per export file; never removed, as removing safely would need its own locking
    private static final ConcurrentHashMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    protected final GedcomHolder gedcomHolder;
    protected final FamilyTreeHelper familyTreeHelper;
    protected final GedcomAnalyzerProperties properties;

    private final FamilyTreeType type;
    @Getter
    private final String exportFileExtension;
    @Getter
    private final MediaType exportFileMediaType;

    protected AbstractFamilyTreeService(
            GedcomHolder gedcomHolder,
            FamilyTreeHelper familyTreeHelper,
            GedcomAnalyzerProperties properties,
            FamilyTreeType type,
            String exportFileExtension,
            MediaType exportFileMediaType) {
        this.gedcomHolder = gedcomHolder;
        this.familyTreeHelper = familyTreeHelper;
        this.properties = properties;
        this.type = type;
        this.exportFileExtension = exportFileExtension;
        this.exportFileMediaType = exportFileMediaType;
    }

    /**
     * Writes the export to {@code exportFilePath}, which is a temporary file in the final directory.
     */
    protected abstract void export(
            Path exportFilePath,
            EnrichedPerson person,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            List<List<Relationship>> relationshipsWithNotInLawPriority);

    public static String getFamilyTreeFileSuffix(boolean obfuscateLiving) {
        return obfuscateLiving ? "" : VISIBLE_SUFFIX;
    }

    public Path getExportFilePath(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix) {

        return properties
                .getTempDir()
                .resolve(FAMILY_TREES_DIR)
                .resolve(familyTreeFileIdPrefix + "_" + person.getUuid() + familyTreeFileSuffix + "." + exportFileExtension);
    }

    @Override
    public boolean isMissingFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix) {

        return Files.notExists(getExportFilePath(person, familyTreeFileIdPrefix, familyTreeFileSuffix));
    }

    @Override
    public void generateFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            List<List<Relationship>> relationshipsWithNotInLawPriority) {

        Path exportFilePath = getExportFilePath(person, familyTreeFileIdPrefix, familyTreeFileSuffix);
        Instant requestedAt = Instant.now();
        ReentrantLock lock = LOCKS.computeIfAbsent(exportFilePath, _ -> new ReentrantLock());
        lock.lock();
        try {
            if (wasWrittenSince(exportFilePath, requestedAt)) {
                // Another thread generated it while this one waited for the lock
                return;
            }

            log.info("Generating {} family tree [ personId={}, personUuid={} ]", type, person.getId(), person.getUuid());
            long startTime = System.currentTimeMillis();

            Path tempFilePath = exportFilePath.resolveSibling(
                    ".part-" + Long.toHexString(ThreadLocalRandom.current().nextLong()) + "-" + exportFilePath.getFileName());
            try {
                Files.createDirectories(exportFilePath.getParent());
                export(tempFilePath, person, obfuscateLiving, onlySecondaryDescription, relationshipsWithNotInLawPriority);
                if (Files.exists(tempFilePath)) {
                    Files.move(tempFilePath, exportFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                deleteQuietly(tempFilePath);
            }

            log.info("Completed {} family tree [ personId={}, personUuid={}, ms={} ]",
                    type, person.getId(), person.getUuid(), System.currentTimeMillis() - startTime);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<FamilyTree> getFamilyTree(
            UUID personUuid,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            boolean forceRewrite) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = gedcom.getPersonByUuid(personUuid);
        if (person == null) {
            log.warn("Person not found [ personUuid={}, modifiedDateTime={} ]", personUuid, gedcom.getModifiedDateTime());
            return Optional.empty();
        }

        String familyTreeFileIdPrefix = familyTreeHelper.getFamilyTreeFileId(person);
        String familyTreeFileSuffix = getFamilyTreeFileSuffix(obfuscateLiving);
        Path exportFilePath = getExportFilePath(person, familyTreeFileIdPrefix, familyTreeFileSuffix);

        if (forceRewrite || Files.notExists(exportFilePath)) {
            generateFamilyTree(
                    person,
                    familyTreeFileIdPrefix,
                    familyTreeFileSuffix,
                    obfuscateLiving,
                    onlySecondaryDescription,
                    familyTreeHelper.getRelationshipsWithNotInLawPriority(person));
        }

        return Optional.of(new FamilyTree(
                type,
                person,
                "genea_azul_arbol_" + familyTreeFileIdPrefix + "." + exportFileExtension,
                exportFilePath,
                exportFileMediaType,
                properties.getLocale()));
    }

    private static boolean wasWrittenSince(Path path, Instant instant) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            return !lastModified.toInstant().isBefore(instant);
        } catch (NoSuchFileException e) {
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete temporary family tree file {}", path, e);
        }
    }

}
