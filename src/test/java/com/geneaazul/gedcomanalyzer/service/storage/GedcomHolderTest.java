package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GedcomHolderTest {

    private final StorageService storageService = mock(StorageService.class);
    private final List<Object> events = new CopyOnWriteArrayList<>();
    private final GedcomHolder holder = new GedcomHolder(storageService, events::add);

    @Test
    void notLoadedYetFailsWithServiceUnavailableAfterWaiting() {
        assertThat(holder.isLoaded()).isFalse();
        assertThatThrownBy(() -> holder.getGedcom(Duration.ofMillis(20)))
                .isInstanceOf(GedcomNotLoadedException.class)
                .satisfies(e -> assertThat(((GedcomNotLoadedException) e).getHeaders().getFirst("Retry-After")).isEqualTo("30"));
    }

    @Test
    void waitingReadersGetTheGedcomAsSoonAsItLoads() throws Exception {
        EnrichedGedcom gedcom = mock(EnrichedGedcom.class);
        when(storageService.getGedcom(false)).thenReturn(gedcom);

        CompletableFuture<EnrichedGedcom> reader = CompletableFuture.supplyAsync(() -> holder.getGedcom(Duration.ofSeconds(5)));
        assertThat(holder.reloadFromStorage(false)).isTrue();

        assertThat(reader.get(5, TimeUnit.SECONDS)).isSameAs(gedcom);
    }

    @Test
    void everyReaderSeesTheSameInstanceAndReloadReplacesIt() throws Exception {
        EnrichedGedcom first = mock(EnrichedGedcom.class);
        EnrichedGedcom second = mock(EnrichedGedcom.class);
        when(storageService.getGedcom(false)).thenReturn(first);
        when(storageService.getGedcom(true)).thenReturn(second);
        holder.reloadFromStorage(false);

        assertThat(holder.getGedcom()).isSameAs(first);
        assertThat(holder.getGedcom()).isSameAs(first);

        holder.reloadFromStorage(true);
        assertThat(holder.getGedcom()).isSameAs(second);
        assertThat(holder.getGedcom()).isSameAs(second);
        assertThat(events).containsExactly(new GedcomLoadedEvent(first), new GedcomLoadedEvent(second));
    }

    @Test
    void aFailedRefreshFallsBackToTheCachedGedcom() throws Exception {
        EnrichedGedcom cached = mock(EnrichedGedcom.class);
        when(storageService.getGedcom(true)).thenThrow(new IllegalStateException("drive down"));
        when(storageService.getGedcom(false)).thenReturn(cached);

        assertThat(holder.reloadFromStorage(true)).isFalse();
        assertThat(holder.getGedcom()).isSameAs(cached);
        assertThat(events).containsExactly(new GedcomLoadedEvent(cached));
    }

    @Test
    void aSecondReloadWhileOneIsRunningIsRejected() throws Exception {
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        EnrichedGedcom gedcom = mock(EnrichedGedcom.class);
        when(storageService.getGedcom(true)).thenAnswer(invocation -> {
            loading.countDown();
            release.await(5, TimeUnit.SECONDS);
            return gedcom;
        });

        CompletableFuture<Boolean> firstReload = CompletableFuture.supplyAsync(() -> holder.reloadFromStorage(true));
        assertThat(loading.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> holder.reloadFromStorage(true)).isInstanceOf(ReloadInProgressException.class);

        release.countDown();
        assertThat(firstReload.get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(holder.getGedcom()).isSameAs(gedcom);
    }

}
