package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
 * Deletes old family-tree exports (.csv, .html, .json, .pdf, .txt) from the temp directory.
 *
 * <p>Person UUIDs change on every GEDCOM import, so older exports can no longer be requested.
 * Runs at startup as well as daily: the Fly machine is suspended most of the time and
 * misses scheduled runs, but every deploy or restart goes through startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FamilyTreeCleanupScheduler {

    static final int RETENTION_DAYS = 30;
    private static final List<String> CLEANUP_EXTENSIONS = List.of(".csv", ".html", ".json", ".pdf", ".txt");

    private final GedcomAnalyzerProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 3 * * *")
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
