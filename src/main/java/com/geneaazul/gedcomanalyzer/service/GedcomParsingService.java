package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.Aka;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.ProfilePicture;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.AlivePersonFilter;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto.PersonNodeDto;
import com.geneaazul.gedcomanalyzer.utils.FamilyUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.folg.gedcom.model.CharacterSet;
import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.DateTime;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.GedcomVersion;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import org.folg.gedcom.parser.ModelParser;
import org.folg.gedcom.visitors.GedcomWriter;
import org.xml.sax.SAXParseException;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import jakarta.annotation.Nullable;

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

    /**
     * Builds a filtered sub-GEDCOM from the given relationships list.
     *
     * <p>Trimming is active when {@code trimTriggerSize} is non-null and the tree size
     * exceeds it (or when the trigger is {@code 0}, which is always exceeded). When trimming is
     * active, a person is kept if either of these two clauses holds
     * ({@code asc = distanceToAncestorRootPerson}, {@code desc = distanceToAncestorThisPerson}):
     * <pre>
     *   Clause 1 (main range):
     *     asc &lt;= maxAscDepth  AND  desc &lt;= maxDescDepth
     *
     *   Clause 2 (ancestor overflow, only when distantAncestorDescLimit is non-null):
     *     asc &gt; maxAscDepth  AND  desc &lt;= distantAncestorDescLimit
     * </pre>
     * Clause 1 caps both the root person's ancestors and their extended descendants.
     * Clause 2 re-admits ancestors beyond the main asc cap, but restricts their family to a
     * tight desc window (typically 0 or 1) so their full subtree is not pulled in.
     * Set {@code distantAncestorDescLimit = null} to disable clause 2 entirely.
     *
     * @param gedcom the source GEDCOM
     * @param relationshipsList one entry per person, each a list of relationship alternatives
     * @param alivePersonFilter how to handle living persons
     * @param displayOnlyBasic if true, copy only essential event tags (birth, death, marriage…) and basic name fields; notes on events are preserved either way
     * @param directLineageOnly if true, exclude collateral relatives (siblings, cousins, etc.)
     * @param rootPersonId if non-null, written as {@code _ROOT} in the output header
     * @param trimTriggerSize null = never trim; 0 = always trim; N &gt; 0 = trim only when the tree exceeds N people
     * @param maxAscDepth max ancestor levels included in the main range
     * @param maxDescDepth max descendant levels included in the main range
     * @param distantAncestorDescLimit desc cap for ancestors beyond the main asc range (null = disabled, i.e. no ancestor overflow)
     * @param includeInLawsAtMaxDescDepth if false, in-law relatives at exactly {@code desc == maxDescDepth} are excluded
     * @param includeInLawsAtMaxAscDepth if false, in-law relatives at exactly {@code asc == maxAscDepth} are excluded
     * @param maxCollateralDescDepth null = no restriction on collateral relatives; N &gt;= 0 = exclude all
     *                               non-direct in-law entries and restrict non-direct blood entries to
     *                               {@code desc &le; N}; has no effect when {@code directLineageOnly} is true
     * @return filtered sub-GEDCOM ready to be written
     */
    public Gedcom format(
            Gedcom gedcom,
            List<List<Relationship>> relationshipsList,
            AlivePersonFilter alivePersonFilter,
            boolean displayOnlyBasic,
            boolean directLineageOnly,
            @Nullable Integer rootPersonId,
            @Nullable Integer trimTriggerSize,
            int maxAscDepth,
            int maxDescDepth,
            @Nullable Integer distantAncestorDescLimit,
            boolean includeInLawsAtMaxDescDepth,
            boolean includeInLawsAtMaxAscDepth,
            @Nullable Integer maxCollateralDescDepth) {
        log.info("Format gedcom: people in tree: {}, max people threshold: {}",
                relationshipsList.size(), trimTriggerSize);

        if (maxAscDepth < 0) {
            throw new IllegalArgumentException("maxAscDepth must be >= 0: " + maxAscDepth);
        }
        if (maxDescDepth < 0) {
            throw new IllegalArgumentException("maxDescDepth must be >= 0: " + maxDescDepth);
        }
        if (trimTriggerSize != null && trimTriggerSize < 0) {
            throw new IllegalArgumentException("trimTriggerSize must be >= 0 when non-null: " + trimTriggerSize);
        }
        if (distantAncestorDescLimit != null && distantAncestorDescLimit < 0) {
            throw new IllegalArgumentException("distantAncestorDescLimit must be >= 0 when non-null: " + distantAncestorDescLimit);
        }
        if (distantAncestorDescLimit != null && distantAncestorDescLimit >= maxDescDepth) {
            throw new IllegalArgumentException(
                    "distantAncestorDescLimit must be < maxDescDepth when non-null: "
                            + distantAncestorDescLimit + " >= " + maxDescDepth);
        }
        if (distantAncestorDescLimit != null && maxAscDepth == 0) {
            throw new IllegalArgumentException(
                    "distantAncestorDescLimit requires maxAscDepth > 0: with maxAscDepth=0 every ancestor"
                            + " satisfies asc > 0 so clause 2 would apply to all of them, making maxAscDepth meaningless");
        }
        if (maxCollateralDescDepth != null && maxCollateralDescDepth < 0) {
            throw new IllegalArgumentException(
                    "maxCollateralDescDepth must be >= 0 when non-null: " + maxCollateralDescDepth);
        }

        boolean trimGedcom = trimTriggerSize != null
                && relationshipsList.size() > trimTriggerSize;

        Set<String> personIds = relationshipsList
                .stream()
                .filter(l -> alivePersonFilter != AlivePersonFilter.SKIP || !l.getFirst().person().isAlive())
                .filter(l -> !directLineageOnly || l.getFirst().isDirect())
                .filter(l -> maxCollateralDescDepth == null
                        || l.getFirst().isDirect()
                        || (!l.getFirst().isInLaw()
                                && l.getFirst().distanceToAncestorThisPerson() <= maxCollateralDescDepth))
                .filter(l -> !trimGedcom
                        || l.getFirst().distanceToAncestorRootPerson() <= maxAscDepth
                                && l.getFirst().distanceToAncestorThisPerson() <= maxDescDepth
                                && (includeInLawsAtMaxDescDepth
                                        || l.getFirst().distanceToAncestorThisPerson() < maxDescDepth
                                        || !l.getFirst().isInLaw())
                                && (includeInLawsAtMaxAscDepth
                                        || l.getFirst().distanceToAncestorRootPerson() < maxAscDepth
                                        || !l.getFirst().isInLaw())
                        || distantAncestorDescLimit != null
                                && l.getFirst().distanceToAncestorRootPerson() > maxAscDepth
                                && l.getFirst().distanceToAncestorThisPerson() <= distantAncestorDescLimit)
                .map(List::getFirst)
                .map(Relationship::person)
                .map(EnrichedPerson::getId)
                .map(id -> "I" + id)
                .collect(Collectors.toUnmodifiableSet());

        Set<String> alivePersonIds = alivePersonFilter == AlivePersonFilter.SHOW_SURNAME_ONLY
                ? relationshipsList.stream()
                        .map(List::getFirst)
                        .map(Relationship::person)
                        .filter(EnrichedPerson::isAlive)
                        .map(ep -> "I" + ep.getId())
                        .filter(personIds::contains)
                        .collect(Collectors.toUnmodifiableSet())
                : Set.of();

        List<Family> families = gedcom.getFamilies()
                .stream()
                .map(family -> {
                    List<SpouseRef> husbandRefs = family.getHusbandRefs().stream().filter(ref -> personIds.contains(ref.getRef())).toList();
                    List<SpouseRef> wifeRefs = family.getWifeRefs().stream().filter(ref -> personIds.contains(ref.getRef())).toList();
                    List<ChildRef> childRefs = family.getChildRefs().stream().filter(ref -> personIds.contains(ref.getRef())).toList();
                    boolean stripFamilyEvents = alivePersonFilter == AlivePersonFilter.SHOW_SURNAME_ONLY
                            && !husbandRefs.isEmpty()
                            && !wifeRefs.isEmpty()
                            && husbandRefs.stream().allMatch(ref -> alivePersonIds.contains(ref.getRef()))
                            && wifeRefs.stream().allMatch(ref -> alivePersonIds.contains(ref.getRef()));
                    return copyFamily(family, husbandRefs, wifeRefs, childRefs, displayOnlyBasic, stripFamilyEvents);
                })
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
                        person.getSpouseFamilyRefs().stream().filter(ref -> familyIds.contains(ref.getRef())).toList(),
                        alivePersonIds.contains(person.getId()),
                        displayOnlyBasic))
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
        newGedcom.setNotes(gedcom.getNotes());
        newGedcom.createIndexes();
        newGedcom.updateReferences();

        if (rootPersonId != null) {
            setRootPerson(newGedcom, rootPersonId);
        }

        if (trimGedcom) {
            log.warn("Gedcom was trimmed! people in tree: {}, max people threshold: {}, final people in tree: {}",
                    relationshipsList.size(), trimTriggerSize, personIds.size());
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

    private void setRootPerson(Gedcom gedcom, int personId) {
        GedcomTag rootTag = new GedcomTag(null, "_ROOT", "I" + personId);
        @SuppressWarnings("unchecked")
        List<GedcomTag> existing = (List<GedcomTag>) gedcom.getHeader().getExtension(ModelParser.MORE_TAGS_EXTENSION_KEY);
        List<GedcomTag> moreTags = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
        moreTags.add(rootTag);
        gedcom.getHeader().putExtension(ModelParser.MORE_TAGS_EXTENSION_KEY, moreTags);
    }

    private static Family copyFamily(
            Family src,
            List<SpouseRef> husbandRefs,
            List<SpouseRef> wifeRefs,
            List<ChildRef> childRefs,
            boolean displayOnlyBasic,
            boolean stripFamilyEvents) {
        Family copy = new Family();
        copy.setId(src.getId());
        copy.setHusbandRefs(husbandRefs);
        copy.setWifeRefs(wifeRefs);
        copy.setChildRefs(childRefs);
        if (stripFamilyEvents) {
            // no events
        } else if (displayOnlyBasic) {
            copy.setEventsFacts(basicFamilyEventFacts(src.getEventsFacts()));
        } else {
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
        }
        return copy;
    }

    private static Person copyPerson(
            Person src,
            List<ParentFamilyRef> parentFamilyRefs,
            List<SpouseFamilyRef> spouseFamilyRefs,
            boolean showSurnameOnly,
            boolean displayOnlyBasic) {
        Person copy = new Person();
        copy.setId(src.getId());
        copy.setParentFamilyRefs(parentFamilyRefs);
        copy.setSpouseFamilyRefs(spouseFamilyRefs);
        if (showSurnameOnly) {
            copy.setNames(surnameOnlyNames(src.getNames()));
        } else if (displayOnlyBasic) {
            copy.setNames(basicNames(src.getNames()));
            copy.setEventsFacts(basicPersonEventFacts(src.getEventsFacts()));
            copy.setUid(src.getUid());
            copy.setUidTag(src.getUidTag());
        } else {
            copy.setNames(src.getNames());
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
        }
        return copy;
    }

    private static List<Name> surnameOnlyNames(List<Name> names) {
        return names.stream()
                .map(n -> {
                    Name copy = new Name();
                    String surname = n.getSurname();
                    if (StringUtils.isNotBlank(surname)) {
                        copy.setValue("<privado> /" + surname + "/");
                    } else {
                        copy.setValue("<privado>");
                    }
                    copy.setGiven("<privado>");
                    copy.setSurnamePrefix(n.getSurnamePrefix());
                    copy.setSurname(n.getSurname());
                    return copy;
                })
                .toList();
    }

    private static List<Name> basicNames(List<Name> names) {
        return names.stream()
                .map(n -> {
                    Name copy = new Name();
                    copy.setValue(n.getValue());
                    copy.setPrefix(n.getPrefix());
                    copy.setGiven(n.getGiven());
                    copy.setNickname(n.getNickname());
                    copy.setSurnamePrefix(n.getSurnamePrefix());
                    copy.setSurname(n.getSurname());
                    copy.setSuffix(n.getSuffix());
                    return copy;
                })
                .toList();
    }

    private static List<EventFact> basicPersonEventFacts(List<EventFact> eventFacts) {
        return eventFacts.stream()
                .filter(ef -> PersonUtils.SEX_TAGS.contains(ef.getTag())
                        || PersonUtils.BIRTH_TAGS.contains(ef.getTag())
                        || PersonUtils.BAPTISM_TAGS.contains(ef.getTag())
                        || PersonUtils.CHRISTENING_TAGS.contains(ef.getTag())
                        || PersonUtils.DEATH_TAGS.contains(ef.getTag())
                        || PersonUtils.BURIAL_TAGS.contains(ef.getTag()))
                .map(GedcomParsingService::basicEventFact)
                .toList();
    }

    private static List<EventFact> basicFamilyEventFacts(List<EventFact> eventFacts) {
        return eventFacts.stream()
                .filter(ef -> FamilyUtils.MARRIAGE_TAGS.contains(ef.getTag())
                        || FamilyUtils.CIVIL_MARRIAGE_TAGS.contains(ef.getTag())
                        || FamilyUtils.OTHER_MARRIAGE_TAGS.contains(ef.getTag())
                        || FamilyUtils.DIVORCE_TAGS.contains(ef.getTag())
                        || FamilyUtils.EVENT_TAGS.contains(ef.getTag())
                                && (FamilyUtils.PARTNERS_EVENT_TYPES.contains(ef.getType())
                                        || FamilyUtils.SEPARATION_EVENT_TYPES.contains(ef.getType())))
                .map(GedcomParsingService::basicEventFact)
                .toList();
    }

    private static EventFact basicEventFact(EventFact src) {
        EventFact copy = new EventFact();
        copy.setTag(src.getTag());
        copy.setType(src.getType());
        copy.setValue(src.getValue());
        copy.setDate(src.getDate());
        copy.setPlace(src.getPlace());
        copy.setNotes(src.getNotes());
        copy.setNoteRefs(src.getNoteRefs());
        return copy;
    }

}

