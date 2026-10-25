package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.http.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractFamilyTreeServiceTest {

    @TempDir
    Path tempDir;

    private EnrichedPerson person;
    private TestService service;

    private static class TestService extends AbstractFamilyTreeService {

        final AtomicInteger exports = new AtomicInteger();
        volatile CountDownLatch insideExport = new CountDownLatch(0);
        volatile CountDownLatch releaseExport = new CountDownLatch(0);
        volatile RuntimeException failure;
        volatile Path lastTarget;

        TestService(GedcomAnalyzerProperties properties) {
            super(mock(GedcomHolder.class), mock(FamilyTreeHelper.class), properties, FamilyTreeType.PLAIN_TXT, "txt", MediaType.TEXT_PLAIN);
        }

        @Override
        protected void export(Path exportFilePath, EnrichedPerson person, boolean obfuscateLiving,
                              boolean onlySecondaryDescription, List<List<Relationship>> relationships) {
            exports.incrementAndGet();
            lastTarget = exportFilePath;
            try {
                Files.writeString(exportFilePath, "partial");
                insideExport.countDown();
                releaseExport.await(5, TimeUnit.SECONDS);
                if (failure != null) {
                    throw failure;
                }
                Files.writeString(exportFilePath, "complete");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @BeforeEach
    void setUp() {
        GedcomAnalyzerProperties properties = mock(GedcomAnalyzerProperties.class);
        when(properties.getTempDir()).thenReturn(tempDir);
        person = mock(EnrichedPerson.class);
        when(person.getUuid()).thenReturn(UUID.fromString("00000000-0000-8000-8000-000000000001"));
        service = new TestService(properties);
    }

    private void generate() {
        service.generateFamilyTree(person, "juan_perez", "", true, true, List.of());
    }

    @Test
    void theFinalFileOnlyAppearsOnceCompleteAndTheTemporaryOneIsGone() throws Exception {
        service.insideExport = new CountDownLatch(1);
        service.releaseExport = new CountDownLatch(1);
        Path finalPath = service.getExportFilePath(person, "juan_perez", "");

        CompletableFuture<Void> generation = CompletableFuture.runAsync(this::generate);
        assertThat(service.insideExport.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(finalPath).doesNotExist();
        assertThat(service.lastTarget.getParent()).isEqualTo(finalPath.getParent());
        assertThat(service.lastTarget).isNotEqualTo(finalPath);

        service.releaseExport.countDown();
        generation.get(5, TimeUnit.SECONDS);

        assertThat(Files.readString(finalPath)).isEqualTo("complete");
        assertThat(Files.list(finalPath.getParent())).containsExactly(finalPath);
    }

    @Test
    void concurrentRequestsForTheSameFileExportOnce() throws Exception {
        service.insideExport = new CountDownLatch(1);
        service.releaseExport = new CountDownLatch(1);

        CompletableFuture<Void> first = CompletableFuture.runAsync(this::generate);
        assertThat(service.insideExport.await(5, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<Void> second = CompletableFuture.runAsync(this::generate);
        CompletableFuture<Void> third = CompletableFuture.runAsync(this::generate);

        service.releaseExport.countDown();
        CompletableFuture.allOf(first, second, third).get(5, TimeUnit.SECONDS);

        assertThat(service.exports).hasValue(1);
    }

    @Test
    void aFailedExportKeepsThePreviousFileAndLeavesNoTemporaryFile() throws Exception {
        Path finalPath = service.getExportFilePath(person, "juan_perez", "");
        Files.createDirectories(finalPath.getParent());
        Files.writeString(finalPath, "previous");
        Files.setLastModifiedTime(finalPath, java.nio.file.attribute.FileTime.fromMillis(0));
        service.failure = new IllegalStateException("export failed");

        assertThatThrownBy(this::generate).hasMessage("export failed");

        assertThat(Files.readString(finalPath)).isEqualTo("previous");
        assertThat(Files.list(finalPath.getParent())).containsExactly(finalPath);
    }

    @Test
    void obfuscatedAndVisibleExportsUseDifferentFiles() {
        assertThat(AbstractFamilyTreeService.getFamilyTreeFileSuffix(true)).isEmpty();
        assertThat(AbstractFamilyTreeService.getFamilyTreeFileSuffix(false)).isEqualTo("_visible");
        assertThat(service.getExportFilePath(person, "juan_perez", "_visible").getFileName().toString())
                .isEqualTo("juan_perez_00000000-0000-8000-8000-000000000001_visible.txt");
    }

}
