package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GoogleCloudStorageService implements StorageService {

    private final GedcomParsingService gedcomParsingService;
    private final GedcomAnalyzerProperties properties;

    @Override
    public EnrichedGedcom getGedcom() throws Exception {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        Blob gedcomBlob = storage.get(properties.getGoogleStorageBucketName(), properties.getGoogleStorageGedcomBlobName());
        return gedcomParsingService.parse(gedcomBlob.getContent(), properties.getGoogleStorageGedcomBlobName());
    }

    @Override
    public String getGedcomName() {
        return properties.getGoogleStorageBucketName() + "/" + properties.getGoogleStorageGedcomBlobName();
    }

}
