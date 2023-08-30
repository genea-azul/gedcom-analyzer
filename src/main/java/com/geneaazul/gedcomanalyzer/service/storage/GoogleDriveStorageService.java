package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

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

        try (FileOutputStream outputStream = new FileOutputStream(properties.getGedcomStorageLocalPath().toFile())) {
            service
                    .files()
                    .get(properties.getGedcomStorageGoogleDriveFileId())
                    .executeMediaAndDownloadTo(outputStream);
        }
    }

    @Override
    public String getGedcomName() {
        return "Google Drive file " + properties.getGedcomStorageGoogleDriveFileId();
    }

}
