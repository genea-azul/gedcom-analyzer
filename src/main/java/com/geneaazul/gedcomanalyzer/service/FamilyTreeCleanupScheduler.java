package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job that deletes old family-tree export files (.csv, .html, .pdf)
 * from the temp directory. Runs once a week; files older than 120 days are removed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FamilyTreeCleanupScheduler {

    private static final int RETENTION_DAYS = 120;
    private static final List<String> CLEANUP_EXTENSIONS = List.of(".csv", ".html", ".pdf");

    private final GedcomAnalyzerProperties properties;

    /**
     * Runs every Sunday at 02:00 (server timezone).
     * Spring cron: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void deleteOldFamilyTreeFiles() {
        Path familyTreesDir = properties.getTempDir().resolve("family-trees");
        if (!Files.isDirectory(familyTreesDir)) {
            log.debug("Family trees directory does not exist, skipping cleanup: {}", familyTreesDir);
            return;
        }

        Instant cutoff = Instant.now().minusSeconds((long) RETENTION_DAYS * 24 * 60 * 60);
        int deletedCount = 0;

        try (Stream<Path> paths = Files.walk(familyTreesDir)
                .filter(Files::isRegularFile)
                .filter(this::hasCleanupExtension)
                .filter(p -> getLastModifiedInstant(p).isBefore(cutoff))) {

            for (Path path : paths.toList()) {
                try {
                    Files.delete(path);
                    deletedCount++;
                    log.debug("Deleted old family tree file: {}", path);
                } catch (IOException e) {
                    log.warn("Failed to delete old family tree file: {}", path, e);
                }
            }
        } catch (IOException e) {
            log.error("Error while cleaning old family tree files in {}", familyTreesDir, e);
            return;
        }

        if (deletedCount > 0) {
            log.info("Family tree cleanup: deleted {} file(s) older than {} days in {}", deletedCount, RETENTION_DAYS, familyTreesDir);
        }
    }

    private boolean hasCleanupExtension(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return CLEANUP_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private static Instant getLastModifiedInstant(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            return Instant.MAX; // skip deletion if we can't read the time
        }
    }
}
