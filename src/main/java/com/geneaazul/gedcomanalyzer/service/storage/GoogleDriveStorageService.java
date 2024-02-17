package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GoogleDriveStorageService implements StorageService {

    private final LocalStorageService localStorageService;
    private final GedcomAnalyzerProperties properties;

    @Override
    public EnrichedGedcom getGedcom(boolean refreshCachedGedcom) throws Exception {

        if (refreshCachedGedcom
                || Files.notExists(properties.getGedcomStorageLocalPath())
                || Files.size(properties.getGedcomStorageLocalPath()) == 0L) {
            log.info("Downloading Gedcom file from Google Drive with id {}", properties.getGedcomStorageGoogleDriveFileId());
            downloadDriveFileToLocalFile();
        }

        return localStorageService.getGedcom();
    }

    private void downloadDriveFileToLocalFile() throws IOException {
        // Build a new unauthorized API client service.
        Drive service = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                request -> request.setInterceptor(intercepted ->
                        intercepted
                                .getUrl()
                                .set("key", properties.getGoogleApiKey())))
                .setApplicationName("gedcom-analyzer")
                .build();

        String mediaType = service
                .files()
                .get(properties.getGedcomStorageGoogleDriveFileId())
                .executeMedia()
                .getMediaType()
                .build();

        // Note: Google Drive considers .ged files media-type as: 'application/octet-stream'
        boolean isCompressed = GedcomParsingService.ZIP_FILE_CONTENT_TYPES.contains(mediaType);

        Path downloadFilePath = isCompressed
                ? Paths.get(StringUtils.replaceOnce(
                        properties.getGedcomStorageLocalPath().toString(),
                        GedcomParsingService.GEDCOM_FILE_EXTENSION,
                        GedcomParsingService.ZIP_FILE_EXTENSION))
                : properties.getGedcomStorageLocalPath();

        log.info("Downloaded file [ mediaType={}, path={} ]", mediaType, downloadFilePath);

        try (FileOutputStream outputStream = new FileOutputStream(downloadFilePath.toFile())) {
            service
                    .files()
                    .get(properties.getGedcomStorageGoogleDriveFileId())
                    .executeMediaAndDownloadTo(outputStream);
        }

        if (isCompressed) {
            try (FileInputStream fis = new FileInputStream(downloadFilePath.toFile());
                    ZipInputStream zis = new ZipInputStream(fis)) {
                ZipEntry zipEntry = zis.getNextEntry();

                if (zipEntry == null) {
                    throw new ZipException("zip file is empty: " + downloadFilePath);
                }
                if (StringUtils.isBlank(zipEntry.getName())
                        || !zipEntry.getName().endsWith(GedcomParsingService.GEDCOM_FILE_EXTENSION)) {
                    throw new ZipException("zip content is invalid: " + zipEntry.getName());
                }

                Path gedcomPath = downloadFilePath.getParent().resolve(zipEntry.getName());
                Files.copy(zis, gedcomPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Decompressed file [ path={} ]", gedcomPath);

                zis.closeEntry();
            }
        }
    }

    @Override
    public String getGedcomName() {
        return "Google Drive file " + properties.getGedcomStorageGoogleDriveFileId();
    }

}
