package com.geneaazul.gedcomanalyzer.service.storage;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reloads the GEDCOM when its source has a newer version (a new MyHeritage export uploaded to Drive).
 *
 * <p>MyHeritage photo URLs expire about a week after the export, so the reload has to happen soon
 * for {@link com.geneaazul.gedcomanalyzer.service.photo.ProfilePictureCache} to store them. The check
 * only runs while the machine is awake, i.e. whenever the site gets visitors.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GedcomAutoReloader {

    private final StorageService storageService;
    private final GedcomHolder gedcomHolder;

    // After a failed reload the restored older copy still looks outdated: only a newer upload is retried
    private final AtomicReference<Instant> failedVersion = new AtomicReference<>();

    @Scheduled(
            initialDelayString = "${gedcom.auto-reload.initial-delay:PT2M}",
            fixedDelayString = "${gedcom.auto-reload.interval:PT1H}")
    public void reloadIfSourceChanged() {
        if (!gedcomHolder.isLoaded()) {
            return;
        }
        try {
            Optional<Instant> newerVersion = storageService.findNewerGedcomVersion();
            if (newerVersion.isEmpty() || newerVersion.get().equals(failedVersion.get())) {
                return;
            }
            log.info("A newer gedcom ({}) is available in {}, reloading", newerVersion.get(), storageService.getGedcomName());
            if (!gedcomHolder.reloadFromStorage(true)) {
                failedVersion.set(newerVersion.get());
                log.warn("Automatic reload of the gedcom from {} failed; it will be retried when a newer one is uploaded", newerVersion.get());
            }
        } catch (ReloadInProgressException e) {
            log.info("Skipping automatic reload: a reload is already running");
        } catch (Exception e) {
            log.warn("Could not check {} for a newer gedcom: {}", storageService.getGedcomName(), e.getMessage());
        }
    }

}
