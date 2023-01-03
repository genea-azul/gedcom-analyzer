package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.parser.ModelParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GedcomParsingService {

    private static final Set<String> ZIP_FILE_CONTENT_TYPES = Set.of("application/zip", "application/octet-stream");
    private static final String ZIP_FILE_EXTENSION = ".zip";
    private static final String GEDCOM_FILE_EXTENSION = ".ged";

    private final GedcomAnalyzerProperties properties;

    public EnrichedGedcom parse(Path gedcomPath) throws IOException, SAXParseException {
        Gedcom gedcom = parseGedcom(gedcomPath.toFile());
        return EnrichedGedcom.of(gedcom, gedcomPath.toString(), properties);
    }

    public EnrichedGedcom parse(byte[] gedcomBytes, String gedcomName) throws SAXParseException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(gedcomBytes);
        Gedcom gedcom = parseGedcom(inputStream);
        return EnrichedGedcom.of(gedcom, gedcomName, properties);
    }

    public EnrichedGedcom parse(MultipartFile gedcomFile) throws IOException, SAXParseException {
        log.info("Upload gedcom: {}", gedcomFile.getOriginalFilename());
        Path gedcomDirPath = null;

        try {
            gedcomDirPath = Files.createTempDirectory(properties.getTempDir(), properties.getTempUploadedGedcomDirPrefix());
            Path gedcomPath = uploadAndDecompress(gedcomDirPath, gedcomFile);
            Gedcom gedcom = parseGedcom(gedcomPath.toFile());
            return EnrichedGedcom.of(gedcom, gedcomFile.getOriginalFilename(), properties);

        } finally {
            if (properties.isDeleteUploadedGedcom() && gedcomDirPath != null) {
                PathUtils.delete(gedcomDirPath);
            }
        }
    }

    private Path uploadAndDecompress(Path gedcomDirPath, MultipartFile uploadedGedcomFile) throws IOException {

        if (uploadedGedcomFile.getOriginalFilename() != null && uploadedGedcomFile.getOriginalFilename().endsWith(ZIP_FILE_EXTENSION)
                && uploadedGedcomFile.getContentType() != null && ZIP_FILE_CONTENT_TYPES.contains(uploadedGedcomFile.getContentType())) {

            try (ZipInputStream zis = new ZipInputStream(uploadedGedcomFile.getInputStream())) {
                ZipEntry zipEntry = zis.getNextEntry();

                if (zipEntry == null) {
                    throw new ZipException("zip file is empty: " + uploadedGedcomFile.getOriginalFilename());
                }
                if (StringUtils.isBlank(zipEntry.getName()) || !zipEntry.getName().endsWith(GEDCOM_FILE_EXTENSION)) {
                    throw new ZipException("zip content is invalid: " + zipEntry.getName());
                }

                Path gedcomPath = gedcomDirPath.resolve(zipEntry.getName());
                Files.copy(zis, gedcomPath, StandardCopyOption.REPLACE_EXISTING);

                zis.closeEntry();
                return gedcomPath;
            }
        }

        if (uploadedGedcomFile.getOriginalFilename() != null && uploadedGedcomFile.getOriginalFilename().endsWith(GEDCOM_FILE_EXTENSION)) {

            try (InputStream is = uploadedGedcomFile.getInputStream()) {
                Path gedcomPath = gedcomDirPath.resolve(Objects.requireNonNullElse(uploadedGedcomFile.getOriginalFilename(), "gedcom.ged"));
                Files.copy(is, gedcomPath, StandardCopyOption.REPLACE_EXISTING);
                return gedcomPath;
            }
        }

        throw new IllegalArgumentException("gedcom file name or content type is invalid: " + uploadedGedcomFile.getOriginalFilename());
    }

    public Gedcom parseGedcom(File gedcomFile) throws IOException, SAXParseException {
        log.info("Parse gedcom file: {}", gedcomFile);
        ModelParser modelParser = new ModelParser();
        Gedcom gedcom = modelParser.parseGedcom(gedcomFile);
        gedcom.createIndexes();
        gedcom.updateReferences();
        return gedcom;
    }

    public Gedcom parseGedcom(InputStream gedcomIs) throws IOException, SAXParseException {
        log.info("Parse gedcom input stream");
        ModelParser modelParser = new ModelParser();
        Gedcom gedcom = modelParser.parseGedcom(gedcomIs);
        gedcom.createIndexes();
        gedcom.updateReferences();
        return gedcom;
    }

}
