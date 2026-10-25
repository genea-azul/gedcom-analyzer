package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Downloads the GEDCOM (plain or zipped) from Google Drive into the local path, then parses it.
 *
 * <p>The local file is the only copy that survives a restart, so it is replaced only by a
 * complete, valid-looking GEDCOM, and the previous file is put back if the new one can't be parsed.
 */
@Slf4j
@RequiredArgsConstructor
public class GoogleDriveStorageService implements StorageService {

    private static final byte[] ZIP_MAGIC = { 'P', 'K', 3, 4 };
    private static final byte[] UTF8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
    private static final byte[] GEDCOM_HEADER = "0 HEAD".getBytes(StandardCharsets.US_ASCII);

    private final LocalStorageService localStorageService;
    private final GedcomAnalyzerProperties properties;

    @Override
    public EnrichedGedcom getGedcom(boolean refreshCachedGedcom) throws Exception {
        Path localPath = properties.getGedcomStorageLocalPath();
        if (!refreshCachedGedcom && Files.exists(localPath) && Files.size(localPath) > 0L) {
            return localStorageService.getGedcom();
        }

        log.info("Downloading Gedcom file from Google Drive with id {}", properties.getGedcomStorageGoogleDriveFileId());
        Path previousPath = localPath.resolveSibling(localPath.getFileName() + ".previous");
        installDownloadedGedcom(localPath, previousPath);

        try {
            return localStorageService.getGedcom();
        } catch (Exception e) {
            if (Files.exists(previousPath)) {
                log.error("Downloaded gedcom could not be parsed, restoring the previous file");
                Files.move(previousPath, localPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            throw e;
        }
    }

    private void installDownloadedGedcom(Path localPath, Path previousPath) throws IOException {
        Path dir = localPath.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path download = Files.createTempFile(dir, "gedcom-download-", ".tmp");
        Path gedcom = Files.createTempFile(dir, "gedcom-download-", ".ged");
        try {
            try (OutputStream out = Files.newOutputStream(download)) {
                downloadDriveFile(out);
            }
            if (startsWith(download, ZIP_MAGIC)) {
                extractSingleGedcom(download, gedcom);
            } else {
                Files.move(download, gedcom, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!looksLikeGedcom(gedcom)) {
                throw new IOException("Downloaded file is not a GEDCOM: " + properties.getGedcomStorageGoogleDriveFileId());
            }
            log.info("Downloaded file [ size={}, path={} ]", Files.size(gedcom), localPath);

            if (Files.exists(localPath)) {
                Files.move(localPath, previousPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            Files.move(gedcom, localPath, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(download);
            Files.deleteIfExists(gedcom);
        }
    }

    protected void downloadDriveFile(OutputStream outputStream) throws IOException {
        int connectTimeoutMillis = Math.toIntExact(properties.getGoogleDriveConnectTimeout().toMillis());
        int readTimeoutMillis = Math.toIntExact(properties.getGoogleDriveReadTimeout().toMillis());
        // Public file read with an API key: no OAuth credentials needed
        Drive drive = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                request -> request
                        .setConnectTimeout(connectTimeoutMillis)
                        .setReadTimeout(readTimeoutMillis)
                        .setInterceptor(intercepted -> intercepted
                                .getUrl()
                                .set("key", properties.getGoogleApiKey())))
                .setApplicationName("gedcom-analyzer")
                .build();

        drive
                .files()
                .get(properties.getGedcomStorageGoogleDriveFileId())
                .executeMediaAndDownloadTo(outputStream);
    }

    private static void extractSingleGedcom(Path zip, Path target) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(GedcomParsingService.GEDCOM_FILE_EXTENSION)) {
                    // The entry name is ignored on purpose: the configured local path is what gets parsed
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        }
        throw new ZipException("zip file has no " + GedcomParsingService.GEDCOM_FILE_EXTENSION + " entry");
    }

    static boolean looksLikeGedcom(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] head = in.readNBytes(UTF8_BOM.length + GEDCOM_HEADER.length);
            int offset = startsWith(head, UTF8_BOM) ? UTF8_BOM.length : 0;
            return head.length - offset >= GEDCOM_HEADER.length
                    && Arrays.equals(head, offset, offset + GEDCOM_HEADER.length, GEDCOM_HEADER, 0, GEDCOM_HEADER.length);
        }
    }

    private static boolean startsWith(Path path, byte[] prefix) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return startsWith(in.readNBytes(prefix.length), prefix);
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }

    @Override
    public String getGedcomName() {
        return "Google Drive file " + properties.getGedcomStorageGoogleDriveFileId();
    }

}
