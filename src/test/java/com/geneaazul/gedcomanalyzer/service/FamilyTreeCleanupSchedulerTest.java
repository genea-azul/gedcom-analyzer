package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FamilyTreeCleanupSchedulerTest {

    @TempDir
    Path tempDir;

    private GedcomAnalyzerProperties properties;
    private FamilyTreeCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = mock(GedcomAnalyzerProperties.class);
        when(properties.getTempDir()).thenReturn(tempDir);
        scheduler = new FamilyTreeCleanupScheduler(properties);
    }

    @Test
    void deleteOldFamilyTreeFiles_deletesOnlyOldCsvHtmlPdf() throws Exception {
        Path familyTreesDir = tempDir.resolve("family-trees");
        Files.createDirectories(familyTreesDir);

        Instant oldInstant = Instant.now().minus(121, ChronoUnit.DAYS);

        Path oldCsv = familyTreesDir.resolve("old.csv");
        Path oldHtml = familyTreesDir.resolve("old.html");
        Path oldPdf = familyTreesDir.resolve("old.pdf");
        createFileWithAge(oldCsv, oldInstant);
        createFileWithAge(oldHtml, oldInstant);
        createFileWithAge(oldPdf, oldInstant);

        Path newCsv = familyTreesDir.resolve("new.csv");
        Path newHtml = familyTreesDir.resolve("new.html");
        Path newPdf = familyTreesDir.resolve("new.pdf");
        createFileWithAge(newCsv, Instant.now());
        createFileWithAge(newHtml, Instant.now());
        createFileWithAge(newPdf, Instant.now());

        Path oldTxt = familyTreesDir.resolve("old.txt");
        createFileWithAge(oldTxt, oldInstant);

        scheduler.deleteOldFamilyTreeFiles();

        assertThat(oldCsv).doesNotExist();
        assertThat(oldHtml).doesNotExist();
        assertThat(oldPdf).doesNotExist();
        assertThat(newCsv).exists();
        assertThat(newHtml).exists();
        assertThat(newPdf).exists();
        assertThat(oldTxt).exists();
    }

    @Test
    void deleteOldFamilyTreeFiles_deletesOldFilesInSubdirs() throws Exception {
        Path familyTreesDir = tempDir.resolve("family-trees");
        Path subdir = familyTreesDir.resolve("subdir");
        Files.createDirectories(subdir);

        Instant oldInstant = Instant.now().minus(121, ChronoUnit.DAYS);
        Path oldInSubdir = subdir.resolve("old.csv");
        createFileWithAge(oldInSubdir, oldInstant);

        scheduler.deleteOldFamilyTreeFiles();

        assertThat(oldInSubdir).doesNotExist();
    }

    @Test
    void deleteOldFamilyTreeFiles_whenDirectoryDoesNotExist_doesNothing() {
        // family-trees subdir is never created
        scheduler.deleteOldFamilyTreeFiles();
        assertThat(tempDir.resolve("family-trees")).doesNotExist();
    }

    @Test
    void deleteOldFamilyTreeFiles_whenDirectoryEmpty_doesNothing() throws Exception {
        Path familyTreesDir = tempDir.resolve("family-trees");
        Files.createDirectories(familyTreesDir);

        scheduler.deleteOldFamilyTreeFiles();

        assertThat(familyTreesDir).exists();
        assertThat(Files.list(familyTreesDir)).isEmpty();
    }

    @Test
    void deleteOldFamilyTreeFiles_keepsFilesWithinRetentionPeriod() throws Exception {
        Path familyTreesDir = tempDir.resolve("family-trees");
        Files.createDirectories(familyTreesDir);

        // 119 days ago = within retention; should NOT be deleted
        Instant withinRetention = Instant.now().minus(119, ChronoUnit.DAYS);
        Path withinRetentionFile = familyTreesDir.resolve("recent.csv");
        createFileWithAge(withinRetentionFile, withinRetention);

        scheduler.deleteOldFamilyTreeFiles();

        assertThat(withinRetentionFile).exists();
    }

    private static void createFileWithAge(Path path, Instant lastModified) throws Exception {
        Files.writeString(path, "content");
        Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.from(lastModified));
    }
}
