package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GedcomHolder {

    static final Duration DEFAULT_WAIT = Duration.ofSeconds(30);

    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    private final Object monitor = new Object();
    private final ReentrantLock reloadLock = new ReentrantLock();

    @Nullable
    private volatile EnrichedGedcom gedcom;

    /**
     * Returns the loaded GEDCOM, waiting while it is (re)loading.
     *
     * @throws GedcomNotLoadedException if it is still not available after {@link #DEFAULT_WAIT}
     */
    public EnrichedGedcom getGedcom() {
        return getGedcom(DEFAULT_WAIT);
    }

    public EnrichedGedcom getGedcom(Duration maxWait) {
        EnrichedGedcom current = gedcom;
        if (current != null) {
            return current;
        }

        long deadline = System.nanoTime() + maxWait.toNanos();
        synchronized (monitor) {
            while ((current = gedcom) == null) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new GedcomNotLoadedException();
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(monitor, remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GedcomNotLoadedException();
                }
            }
            return current;
        }
    }

    public boolean isLoaded() {
        return gedcom != null;
    }

    /**
     * Loads the GEDCOM from storage. Concurrent calls are rejected rather than queued.
     *
     * <p>The current GEDCOM is released before loading because two of them don't fit in the
     * production heap; requests wait meanwhile. If a refreshed GEDCOM fails to load, the cached
     * copy is loaded again so the site keeps serving the last good data.
     *
     * @return whether the requested GEDCOM was loaded
     * @throws ReloadInProgressException if another reload is running
     */
    public boolean reloadFromStorage(boolean refreshCachedGedcom) {
        if (!reloadLock.tryLock()) {
            throw new ReloadInProgressException();
        }
        try {
            setGedcom(null);
            Instant start = Instant.now();
            try {
                EnrichedGedcom loaded = storageService.getGedcom(refreshCachedGedcom);
                setGedcom(loaded);
                eventPublisher.publishEvent(new GedcomLoadedEvent(loaded));
                log.info("Gedcom file loaded: {} - total time: {}", storageService.getGedcomName(), Duration.between(start, Instant.now()));
                return true;
            } catch (Throwable e) {
                log.error("Error when loading gedcom file: {}", storageService.getGedcomName(), e);
            }

            if (refreshCachedGedcom) {
                try {
                    EnrichedGedcom restored = storageService.getGedcom(false);
                    setGedcom(restored);
                    eventPublisher.publishEvent(new GedcomLoadedEvent(restored));
                    log.warn("Restored the previously cached gedcom file: {}", storageService.getGedcomName());
                } catch (Throwable e) {
                    log.error("Error when restoring the cached gedcom file: {}", storageService.getGedcomName(), e);
                }
            }
            return false;
        } finally {
            reloadLock.unlock();
        }
    }

    private void setGedcom(@Nullable EnrichedGedcom newGedcom) {
        synchronized (monitor) {
            gedcom = newGedcom;
            monitor.notifyAll();
        }
    }

    @PreDestroy
    public void preDestroy() {
        setGedcom(null);
    }

}
