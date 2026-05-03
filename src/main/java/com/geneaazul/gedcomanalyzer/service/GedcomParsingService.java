package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.Aka;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.ProfilePicture;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto.FamilyNodeDto;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto.PersonNodeDto;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.folg.gedcom.model.CharacterSet;
import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.DateTime;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomVersion;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import org.folg.gedcom.parser.ModelParser;
import org.folg.gedcom.visitors.GedcomWriter;
import org.xml.sax.SAXParseException;

import jakarta.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GedcomParsingService {

    public static final Set<String> ZIP_FILE_CONTENT_TYPES = Set.of("application/zip", "application/x-zip-compressed");
    public static final String ZIP_FILE_EXTENSION = ".zip";
    public static final String GEDCOM_FILE_EXTENSION = ".ged";

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final GedcomAnalyzerProperties properties;
    private final SearchService searchService;

    public EnrichedGedcom parse(Path gedcomPath) throws IOException, SAXParseException {
        Gedcom gedcom = parseGedcom(gedcomPath);
        ZonedDateTime gedcomModifiedTime = Files.getLastModifiedTime(gedcomPath)
                .toInstant()
                .atZone(properties.getZoneId());
        return EnrichedGedcom.of(gedcom, gedcomPath.toString(), gedcomModifiedTime, properties, searchService);
    }

    public EnrichedGedcom parse(byte[] gedcomBytes, String gedcomName) throws SAXParseException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(gedcomBytes);
        Gedcom gedcom = parseGedcom(inputStream);
        return EnrichedGedcom.of(gedcom, gedcomName, properties, searchService);
    }

    public EnrichedGedcom parse(MultipartFile gedcomFile) throws IOException, SAXParseException {
        log.info("Upload gedcom: {}", gedcomFile.getOriginalFilename());
        Path gedcomDirPath = null;

        try {
            // Make sure temp dir exists
            Files.createDirectories(properties.getTempDir());
            // Create a new directory inside the temp dir
            gedcomDirPath = Files.createTempDirectory(properties.getTempDir(), properties.getTempUploadedGedcomDirPrefix());

            Path gedcomPath = uploadAndDecompress(gedcomDirPath, gedcomFile);
            Gedcom gedcom = parseGedcom(gedcomPath);
            return EnrichedGedcom.of(gedcom, gedcomFile.getOriginalFilename(), properties, searchService);

        } finally {
            if (properties.isDeleteUploadedGedcom() && gedcomDirPath != null) {
                PathUtils.delete(gedcomDirPath);
            }
        }
    }

    private Path uploadAndDecompress(Path gedcomDirPath, MultipartFile uploadedGedcomFile) throws IOException {

        if (uploadedGedcomFile.getContentType() != null && ZIP_FILE_CONTENT_TYPES.contains(uploadedGedcomFile.getContentType())) {

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

    public Gedcom parseGedcom(Path gedcomFile) throws IOException, SAXParseException {
        log.info("Parse gedcom file: {}", gedcomFile);
        ModelParser modelParser = new ModelParser();
        Gedcom gedcom = modelParser.parseGedcom(gedcomFile.toFile());
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

    public Gedcom format(
            Gedcom gedcom,
            List<List<Relationship>> relationshipsList,
            int maxPeopleInGedcomThreshold,
            int maxAscDistanceThreshold,
            int maxDescDistanceThreshold) {
        log.info("Format gedcom: people in tree: {}, max people threshold: {}",
                relationshipsList.size(), maxPeopleInGedcomThreshold);

        boolean trimGedcom = maxPeopleInGedcomThreshold > 0 && relationshipsList.size() > maxPeopleInGedcomThreshold;
        boolean includeSpousesOfDescendantsA = true;
        boolean includeSpousesOfDescendantsB = true;

        //noinspection ConstantValue
        Set<String> personIds = relationshipsList
                .stream()
                .filter(l -> !trimGedcom
                        || l.getFirst().distanceToAncestorRootPerson() < maxAscDistanceThreshold
                        || l.getFirst().distanceToAncestorRootPerson() == maxAscDistanceThreshold
                                && (l.getFirst().distanceToAncestorThisPerson() < maxDescDistanceThreshold
                                || l.getFirst().distanceToAncestorThisPerson() == maxDescDistanceThreshold
                                        && (includeSpousesOfDescendantsA || !l.getFirst().isInLaw()))
                        || l.getFirst().distanceToAncestorRootPerson() > maxAscDistanceThreshold
                                && (l.getFirst().distanceToAncestorThisPerson() < 1
                                || l.getFirst().distanceToAncestorThisPerson() == 1
                                        && (includeSpousesOfDescendantsB || !l.getFirst().isInLaw())))
                .map(List::getFirst)
                .map(Relationship::person)
                .map(EnrichedPerson::getId)
                .map(id -> "I" + id)
                .collect(Collectors.toUnmodifiableSet());

        List<Family> families = gedcom.getFamilies()
                .stream()
                .map(family -> copyFamily(
                        family,
                        family.getHusbandRefs().stream().filter(ref -> personIds.contains(ref.getRef())).toList(),
                        family.getWifeRefs().stream().filter(ref -> personIds.contains(ref.getRef())).toList(),
                        family.getChildRefs().stream().filter(ref -> personIds.contains(ref.getRef())).toList()))
                .filter(family -> {
                    if (family.getHusbandRefs().isEmpty() && family.getWifeRefs().isEmpty()) {
                        return false;
                    }
                    if (family.getChildRefs().isEmpty() && (family.getHusbandRefs().size() + family.getWifeRefs().size()) == 1) {
                        return false;
                    }
                    return true;
                })
                .toList();

        Set<String> familyIds = families
                .stream()
                .map(Family::getId)
                .collect(Collectors.toUnmodifiableSet());

        List<Person> people = gedcom.getPeople()
                .stream()
                .filter(person -> personIds.contains(person.getId()))
                .map(person -> copyPerson(
                        person,
                        person.getParentFamilyRefs().stream().filter(ref -> familyIds.contains(ref.getRef())).toList(),
                        person.getSpouseFamilyRefs().stream().filter(ref -> familyIds.contains(ref.getRef())).toList()))
                .toList();

        Header header = new Header();

        CharacterSet charsetSet = new CharacterSet();
        charsetSet.setValue("UTF-8");
        header.setCharacterSet(charsetSet);

        GedcomVersion gedcomVersion = new GedcomVersion();
        gedcomVersion.setVersion("5.5.1");
        gedcomVersion.setForm("LINEAGE-LINKED");
        header.setGedcomVersion(gedcomVersion);

        header.setLanguage("Spanish");

        DateTime dateTime = new DateTime();
        dateTime.setValue(DATE_TIME_FORMATTER.format(LocalDate.now(properties.getZoneId())).toUpperCase());
        header.setDateTime(dateTime);

        Gedcom newGedcom = new Gedcom();
        newGedcom.setHeader(header);
        newGedcom.setPeople(people);
        newGedcom.setFamilies(families);
        newGedcom.createIndexes();
        newGedcom.updateReferences();

        if (trimGedcom) {
            log.warn("Gedcom was trimmed! people in tree: {}, max people threshold: {}, final people in tree: {}",
                    relationshipsList.size(), maxPeopleInGedcomThreshold, personIds.size());
        }

        return newGedcom;
    }

    public static PersonNodeDto toPersonNodeDto(EnrichedPerson ep, @Nullable String relationship, @Nullable Integer generation) {
        Integer yearOfBirth = null;
        Boolean circaBirth = null;
        if (ep.getDateOfBirth().isPresent()) {
            Date dob = ep.getDateOfBirth().get();
            yearOfBirth = dob.getYear().getValue();
            circaBirth = dob.getOperator() == Date.Operator.ABT || dob.getOperator() == Date.Operator.EST;
        }

        Integer yearOfDeath = null;
        Boolean circaDeath = null;
        if (ep.getDateOfDeath().isPresent()) {
            Date dod = ep.getDateOfDeath().get();
            yearOfDeath = dod.getYear().getValue();
            circaDeath = dod.getOperator() == Date.Operator.ABT || dod.getOperator() == Date.Operator.EST;
        }

        return PersonNodeDto.builder()
                .id(ep.getId())
                .displayName(ep.getDisplayName())
                .sex(ep.getSex())
                .aka(ep.getAka().map(Aka::value).orElse(null))
                .profilePicture(ep.getProfilePicture().map(ProfilePicture::file).orElse(null))
                .yearOfBirth(yearOfBirth)
                .circaBirth(circaBirth)
                .yearOfDeath(yearOfDeath)
                .circaDeath(circaDeath)
                .isAlive(ep.isAlive())
                .generation(generation)
                .relationship(relationship)
                .build();
    }

    public void write(Gedcom gedcom, Path gedcomPath) throws IOException {
        log.info("Write gedcom: {}", gedcomPath);
        try (OutputStream out = new FileOutputStream(gedcomPath.toFile())) {
            GedcomWriter writer = new GedcomWriter();
            writer.write(gedcom, out);
        }
    }

    private static Family copyFamily(Family src, List<SpouseRef> husbandRefs, List<SpouseRef> wifeRefs, List<ChildRef> childRefs) {
        Family copy = new Family();
        copy.setId(src.getId());
        copy.setHusbandRefs(husbandRefs);
        copy.setWifeRefs(wifeRefs);
        copy.setChildRefs(childRefs);
        copy.setEventsFacts(src.getEventsFacts());
        copy.setLdsOrdinances(src.getLdsOrdinances());
        copy.setReferenceNumbers(src.getReferenceNumbers());
        copy.setRin(src.getRin());
        copy.setChange(src.getChange());
        copy.setUid(src.getUid());
        copy.setUidTag(src.getUidTag());
        copy.setSourceCitations(src.getSourceCitations());
        copy.setMediaRefs(src.getMediaRefs());
        copy.setMedia(src.getMedia());
        copy.setNoteRefs(src.getNoteRefs());
        copy.setNotes(src.getNotes());
        Map<String, Object> extensions = src.getExtensions();
        if (extensions != null) {
            copy.setExtensions(extensions);
        }
        return copy;
    }

    private static Person copyPerson(Person src, List<ParentFamilyRef> parentFamilyRefs, List<SpouseFamilyRef> spouseFamilyRefs) {
        Person copy = new Person();
        copy.setId(src.getId());
        copy.setNames(src.getNames());
        copy.setParentFamilyRefs(parentFamilyRefs);
        copy.setSpouseFamilyRefs(spouseFamilyRefs);
        copy.setAssociations(src.getAssociations());
        copy.setAncestorInterestSubmitterRef(src.getAncestorInterestSubmitterRef());
        copy.setDescendantInterestSubmitterRef(src.getDescendantInterestSubmitterRef());
        copy.setRecordFileNumber(src.getRecordFileNumber());
        copy.setAddress(src.getAddress());
        copy.setPhone(src.getPhone());
        copy.setFax(src.getFax());
        copy.setEmail(src.getEmail());
        copy.setEmailTag(src.getEmailTag());
        copy.setWww(src.getWww());
        copy.setWwwTag(src.getWwwTag());
        copy.setEventsFacts(src.getEventsFacts());
        copy.setLdsOrdinances(src.getLdsOrdinances());
        copy.setReferenceNumbers(src.getReferenceNumbers());
        copy.setRin(src.getRin());
        copy.setChange(src.getChange());
        copy.setUid(src.getUid());
        copy.setUidTag(src.getUidTag());
        copy.setSourceCitations(src.getSourceCitations());
        copy.setMediaRefs(src.getMediaRefs());
        copy.setMedia(src.getMedia());
        copy.setNoteRefs(src.getNoteRefs());
        copy.setNotes(src.getNotes());
        Map<String, Object> extensions = src.getExtensions();
        if (extensions != null) {
            copy.setExtensions(extensions);
        }
        return copy;
    }

}
