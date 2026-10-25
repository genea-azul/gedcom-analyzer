package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleDriveStorageServiceTest {

    private static final String NEW_GEDCOM = "﻿0 HEAD\n1 NOTE new\n0 TRLR\n";
    private static final String OLD_GEDCOM = "0 HEAD\n1 NOTE old\n0 TRLR\n";

    @TempDir
    Path tempDir;

    private Path localPath;
    private LocalStorageService localStorageService;
    private GedcomAnalyzerProperties properties;
    private byte[] driveContent;
    private int downloads;

    @BeforeEach
    void setUp() {
        localPath = tempDir.resolve("genea-azul-full-gedcom.ged");
        localStorageService = mock(LocalStorageService.class);
        properties = mock(GedcomAnalyzerProperties.class);
        when(properties.getGedcomStorageLocalPath()).thenReturn(localPath);
        when(properties.getGedcomStorageGoogleDriveFileId()).thenReturn("drive-file-id");
    }

    private Instant driveModifiedTime = Instant.parse("2026-10-25T22:00:00Z");

    private GoogleDriveStorageService service() {
        return new GoogleDriveStorageService(localStorageService, properties) {
            @Override
            protected void downloadDriveFile(OutputStream outputStream) throws IOException {
                downloads++;
                outputStream.write(driveContent);
            }

            @Override
            protected Instant driveFileModifiedTime() {
                return driveModifiedTime;
            }
        };
    }

    private static byte[] zip(String entryName, String content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bytes)) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return bytes.toByteArray();
    }

    @Test
    void usesTheCachedFileWithoutDownloadingWhenNotRefreshing() throws Exception {
        Files.writeString(localPath, OLD_GEDCOM);
        EnrichedGedcom gedcom = mock(EnrichedGedcom.class);
        when(localStorageService.getGedcom()).thenReturn(gedcom);

        assertThat(service().getGedcom(false)).isSameAs(gedcom);
        assertThat(downloads).isZero();
    }

    @Test
    void zippedGedcomIsExtractedToTheConfiguredPathWhateverTheEntryName() throws Exception {
        Files.writeString(localPath, OLD_GEDCOM);
        driveContent = zip("export-2026-10-25.ged", NEW_GEDCOM);

        service().getGedcom(true);

        assertThat(Files.readString(localPath)).isEqualTo(NEW_GEDCOM);
        assertThat(tempDir.resolve("export-2026-10-25.ged")).doesNotExist();
        assertThat(Files.list(tempDir).map(p -> p.getFileName().toString()))
                .containsExactlyInAnyOrder("genea-azul-full-gedcom.ged", "genea-azul-full-gedcom.ged.previous");
    }

    @Test
    void plainGedcomDownloadReplacesTheLocalFile() throws Exception {
        driveContent = NEW_GEDCOM.getBytes(StandardCharsets.UTF_8);

        service().getGedcom(true);

        assertThat(Files.readString(localPath)).isEqualTo(NEW_GEDCOM);
    }

    @Test
    void somethingThatIsNotAGedcomNeverReplacesTheLocalFile() throws Exception {
        Files.writeString(localPath, OLD_GEDCOM);
        driveContent = "<html>Quota exceeded</html>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service().getGedcom(true)).isInstanceOf(IOException.class);

        assertThat(Files.readString(localPath)).isEqualTo(OLD_GEDCOM);
        verify(localStorageService, never()).getGedcom();
        assertThat(Files.list(tempDir)).hasSize(1);
    }

    @Test
    void thePreviousFileIsRestoredWhenTheDownloadedOneCannotBeParsed() throws Exception {
        Files.writeString(localPath, OLD_GEDCOM);
        driveContent = NEW_GEDCOM.getBytes(StandardCharsets.UTF_8);
        when(localStorageService.getGedcom()).thenThrow(new IllegalStateException("broken gedcom"));

        assertThatThrownBy(() -> service().getGedcom(true)).hasMessage("broken gedcom");

        assertThat(Files.readString(localPath)).isEqualTo(OLD_GEDCOM);
    }

    @Test
    void looksLikeGedcomAcceptsAnOptionalUtf8Bom() throws Exception {
        Path withBom = Files.writeString(tempDir.resolve("a.ged"), NEW_GEDCOM);
        Path withoutBom = Files.writeString(tempDir.resolve("b.ged"), OLD_GEDCOM);
        Path empty = Files.writeString(tempDir.resolve("c.ged"), "");

        assertThat(GoogleDriveStorageService.looksLikeGedcom(withBom)).isTrue();
        assertThat(GoogleDriveStorageService.looksLikeGedcom(withoutBom)).isTrue();
        assertThat(GoogleDriveStorageService.looksLikeGedcom(empty)).isFalse();
    }


    @Test
    void aDriveFileModifiedAfterTheLocalCopyIsANewerVersion() throws Exception {
        Files.writeString(localPath, OLD_GEDCOM);
        Files.setLastModifiedTime(localPath, FileTime.from(driveModifiedTime.minusSeconds(60)));

        assertThat(service().findNewerGedcomVersion()).contains(driveModifiedTime);
    }

    @Test
    void aLocalCopyDownloadedAfterTheLastUploadIsCurrent() throws Exception {
        Files.writeString(localPath, OLD_GEDCOM);
        Files.setLastModifiedTime(localPath, FileTime.from(driveModifiedTime.plusSeconds(60)));

        assertThat(service().findNewerGedcomVersion()).isEmpty();
    }

    @Test
    void aMissingLocalCopyMeansTheDriveFileIsNewer() throws Exception {
        assertThat(service().findNewerGedcomVersion()).contains(driveModifiedTime);
    }

}
