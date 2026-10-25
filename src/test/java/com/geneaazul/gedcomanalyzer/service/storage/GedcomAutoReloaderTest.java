package com.geneaazul.gedcomanalyzer.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GedcomAutoReloaderTest {

    private static final Instant UPLOADED = Instant.parse("2026-10-25T22:00:00Z");

    private StorageService storageService;
    private GedcomHolder gedcomHolder;
    private GedcomAutoReloader reloader;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        gedcomHolder = mock(GedcomHolder.class);
        when(gedcomHolder.isLoaded()).thenReturn(true);
        reloader = new GedcomAutoReloader(storageService, gedcomHolder);
    }

    @Test
    void reloadsWhenTheSourceHasANewerGedcom() throws Exception {
        when(storageService.findNewerGedcomVersion()).thenReturn(Optional.of(UPLOADED));
        when(gedcomHolder.reloadFromStorage(true)).thenReturn(true);

        reloader.reloadIfSourceChanged();

        verify(gedcomHolder).reloadFromStorage(true);
    }

    @Test
    void doesNothingWhenTheCachedCopyIsCurrent() throws Exception {
        when(storageService.findNewerGedcomVersion()).thenReturn(Optional.empty());

        reloader.reloadIfSourceChanged();

        verify(gedcomHolder, never()).reloadFromStorage(true);
    }

    @Test
    void doesNotCheckBeforeTheFirstLoad() throws Exception {
        when(gedcomHolder.isLoaded()).thenReturn(false);

        reloader.reloadIfSourceChanged();

        verify(storageService, never()).findNewerGedcomVersion();
    }

    @Test
    void aVersionThatFailedToLoadIsNotRetriedButANewerUploadIs() throws Exception {
        when(storageService.findNewerGedcomVersion()).thenReturn(Optional.of(UPLOADED));
        when(gedcomHolder.reloadFromStorage(true)).thenReturn(false);

        reloader.reloadIfSourceChanged();
        reloader.reloadIfSourceChanged();
        verify(gedcomHolder, times(1)).reloadFromStorage(true);

        when(storageService.findNewerGedcomVersion()).thenReturn(Optional.of(UPLOADED.plusSeconds(3600)));
        reloader.reloadIfSourceChanged();
        verify(gedcomHolder, times(2)).reloadFromStorage(true);
    }

    @Test
    void sourceErrorsAndConcurrentReloadsAreHarmless() throws Exception {
        when(storageService.findNewerGedcomVersion()).thenThrow(new java.io.IOException("Drive unreachable"));
        reloader.reloadIfSourceChanged();

        doReturn(Optional.of(UPLOADED)).when(storageService).findNewerGedcomVersion();
        when(gedcomHolder.reloadFromStorage(true)).thenThrow(new ReloadInProgressException());
        reloader.reloadIfSourceChanged();

        verify(gedcomHolder).reloadFromStorage(true);
    }

}
