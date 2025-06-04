package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.EmbeddedFontsConfig;
import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.FormattedShortestPathDistance;
import com.geneaazul.gedcomanalyzer.model.FormattedShortestPathRelationship;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.RelationshipDto;
import com.geneaazul.gedcomanalyzer.service.PersonService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.PathUtils;
import com.geneaazul.gedcomanalyzer.utils.PlaceUtils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import jakarta.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PlainFamilyTreePdfService extends PlainFamilyTreeService {

    @SuppressWarnings("unused")
    private static final float A4_MAX_OFFSET_X = 590f;
    private static final float A4_MAX_OFFSET_Y = 830f;

    private static final int MAX_DISTANCE_TO_OBFUSCATE = 3;
    private static final int MAX_PERSON_NAME_LENGTH = 48;
    private static final int MAX_DISTINGUISHED_PERSONS_TO_DISPLAY = 92;
    private static final String DATE_TIME_PATTERN = "EEEE d 'de' MMMM 'de' yyyy 'a las' HH:mm:ss 'hs.'";

    // Cacique Cipriano Catriel, Manuel Belgrano, Papa Francisco, Pedro Burgos, Rubén De Paula, Justo José de Urquiza
    private static final List<Integer> SHORTEST_PATH_TARGET_PERSON_IDS = List.of(511668, 543016, 525113, 518817, 505424, 545653);

    private final PersonService personService;
    private final RelationshipMapper relationshipMapper;
    private final Map<EmbeddedFontsConfig.Font, String> embeddedFonts;

    public PlainFamilyTreePdfService(
            GedcomHolder gedcomHolder,
            FamilyTreeHelper familyTreeHelper,
            GedcomAnalyzerProperties properties,
            PersonService personService,
            RelationshipMapper relationshipMapper,
            Map<EmbeddedFontsConfig.Font, String> embeddedFonts) {
        super(
                gedcomHolder,
                familyTreeHelper,
                properties,
                "pdf",
                MediaType.APPLICATION_PDF);
        this.personService = personService;
        this.relationshipMapper = relationshipMapper;
        this.embeddedFonts = embeddedFonts;
    }

    @Override
    protected void export(
            Path exportFilePath,
            EnrichedPerson person,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            List<List<Relationship>> peopleInTree) {
        log.info("Generating plain family tree PDF");

        Set<Integer> distinguishedRelatives = new HashSet<>();
        List<FormattedRelationship> formattedRelationships = peopleInTree
                .stream()
                .peek(relationships -> {
                    if (relationships.getFirst().person().isDistinguishedPerson()) {
                        distinguishedRelatives.add(relationships.getFirst().person().getId());
                    }
                })
                .map(relationships -> relationships
                        .stream()
                        .map(relationship -> relationshipMapper.toRelationshipDto(
                                relationship,
                                getObfuscateCondition(obfuscateLiving, person, relationship)))
                        .map(relationship -> relationshipMapper.formatInSpanish(relationship, onlySecondaryDescription))
                        .toList())
                .map(frs -> frs
                        .stream()
                        .reduce(FormattedRelationship::mergeRelationshipDesc)
                        .orElseThrow())
                .toList();

        Pair<List<FormattedShortestPathDistance>, List<List<FormattedShortestPathRelationship>>> formattedShortestPath =
                calculateFormattedShortestPath(
                        person,
                        distinguishedRelatives,
                        obfuscateLiving,
                        onlySecondaryDescription);

        try {
            exportToPDF(
                    exportFilePath,
                    person,
                    formattedRelationships,
                    formattedShortestPath.getLeft(),
                    formattedShortestPath.getRight());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void exportToPDF(
            Path exportFilePath,
            EnrichedPerson person,
            List<FormattedRelationship> peopleInTree,
            List<FormattedShortestPathDistance> distances,
            List<List<FormattedShortestPathRelationship>> relationshipsList) throws IOException {

        // Create a document
        try (PDDocument document = new PDDocument()) {

            PDFont font = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO);
            PDFont bold = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO_BOLD);
            PDFont light = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO_LIGHT);
            PDFont italic = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO_LIGHT_ITALIC);
            PDFont mono = loadFont(document, EmbeddedFontsConfig.Font.EVERSON_MONO);

            int maxPersonsInFirstPage = 48;
            int maxPersonsInNextPages = 59;

            boolean isAnyPersonObfuscated = peopleInTree
                    .stream()
                    .anyMatch(FormattedRelationship::isObfuscated);

            PDImageXObject logoImage = loadImage(document, "images/logo-bw-small.png");

            writeFirstPage(
                    document,
                    person,
                    peopleInTree,
                    maxPersonsInFirstPage,
                    isAnyPersonObfuscated,
                    logoImage,
                    font,
                    bold,
                    light,
                    italic,
                    mono);

            List<List<FormattedRelationship>> nextPages = (peopleInTree.size() > maxPersonsInFirstPage)
                    ? ListUtils.partition(peopleInTree.subList(maxPersonsInFirstPage, peopleInTree.size()), maxPersonsInNextPages)
                    : List.of();

            for (int i = 0; i < nextPages.size(); i++) {
                List<FormattedRelationship> peopleInPage = nextPages.get(i);
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    writeText(stream, bold, 12.5f, 1.2f, 30f, 40f, "Árbol genealógico de " + person.getDisplayName());

                    writePeopleInPage(
                            stream,
                            font,
                            light,
                            italic,
                            mono,
                            peopleInPage,
                            60f,
                            i + 2,
                            i == nextPages.size() - 1,
                            isAnyPersonObfuscated);
                }
            }

            MutableInt pageNum = new MutableInt(nextPages.size() + 2);

            if (writeDistancesPage(
                    document,
                    distances,
                    pageNum.getValue(),
                    logoImage,
                    font,
                    bold,
                    light,
                    mono)) {
                pageNum.increment();
            }

            relationshipsList
                    .forEach(relationships -> {
                        try {
                            if (writePathToPersonPage(
                                    document,
                                    relationships,
                                    pageNum.getValue(),
                                    logoImage,
                                    font,
                                    bold,
                                    light)) {
                                pageNum.increment();
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

            writeLastPage(
                    document,
                    pageNum.getValue(),
                    logoImage,
                    font,
                    bold,
                    light,
                    italic,
                    mono);

            // Save the document
            Files.createDirectories(exportFilePath.getParent());
            document.save(exportFilePath.toFile());
        }

    }

    private void writeFirstPage(
            PDDocument document,
            EnrichedPerson person,
            List<FormattedRelationship> peopleInTree,
            int maxPersonsInFirstPage,
            boolean isAnyPersonObfuscated,
            PDImageXObject logoImage,
            PDFont font,
            PDFont bold,
            PDFont light,
            PDFont italic,
            PDFont mono) throws IOException {

        PDPage firstPage = new PDPage(PDRectangle.A4);
        document.addPage(firstPage);

        try (PDPageContentStream stream = new PDPageContentStream(document, firstPage)) {

            drawLogoImage(stream, logoImage, A4_MAX_OFFSET_X - 160f, 10f);

            writeText(stream, bold, 16f, 1.2f, 30f, 40f,
                    "Genea Azul");
            writeText(stream, font, 10.5f, 1.2f, 30f, 60f,
                    "sitio web: geneaazul.com.ar  -  en redes sociales: @genea.azul");

            float textPosX = 30f;
            float textPosY = 95f;

            writeText(stream, bold, 12.5f, 1.2f, textPosX, textPosY,
                    "Árbol genealógico de " + person.getDisplayName());

            textPosX = textPosX + 20f;
            textPosY = textPosY + 20f;
            float textSepY = 70f;

            writeText(stream, light, 11f, 1.3f, textPosX, textPosY,
                    "Personas:  " + person.getPersonsCountInTree(),
                    "Apellidos (en caso de apellidos compuestos sólo se considera el primero):  " + person.getSurnamesCountInTree(),
                    "Generaciones:  " + person.getAncestryGenerations().getTotalGenerations()
                            + "  (ascendencia directa: " + person.getAncestryGenerations().ascending()
                            + ", descendencia directa: " + person.getAncestryGenerations().directDescending() + ")",
                    "Países en su ascendencia:  " + (person.getAncestryCountries().isEmpty() ? "-" : String.join(", ", person.getAncestryCountries())));

            writePeopleInPage(
                    stream,
                    font,
                    light,
                    italic,
                    mono,
                    peopleInTree.subList(0, Math.min(peopleInTree.size(), maxPersonsInFirstPage)),
                    textPosY + textSepY,
                    1,
                    peopleInTree.size() <= maxPersonsInFirstPage,
                    isAnyPersonObfuscated);
        }
    }

    private boolean writeDistancesPage(
            PDDocument document,
            List<FormattedShortestPathDistance> distances,
            int pageNum,
            PDImageXObject logoImage,
            PDFont font,
            PDFont bold,
            PDFont light,
            PDFont mono) throws IOException {

        if (distances.isEmpty()) {
            return false;
        }

        PDPage lastPage = new PDPage(PDRectangle.A4);
        document.addPage(lastPage);

        try (PDPageContentStream stream = new PDPageContentStream(document, lastPage)) {

            drawLogoImage(stream, logoImage, A4_MAX_OFFSET_X - 160f, 10f);

            writeText(stream, bold, 16f, 1.2f, 30f, 40f,
                    "Genea Azul");
            writeText(stream, font, 11.5f, 1.2f, 30f, 60f,
                    "sitio web: geneaazul.com.ar  -  en redes sociales: @genea.azul");

            float textPosX = 30f;
            float textPosY = 95f;

            writeText(stream, bold, 12.5f, 1.2f, textPosX, textPosY,
                    "Distancia a personalidades destacadas:");

            textPosX = textPosX + 20f;
            textPosY = textPosY + 20f;
            float textSize = 10f;
            float textParagraphSep = 4f;

            writeText(stream, light, textSize, 1.3f, textPosX, textPosY,
                    "La distancia entre dos personas indica por cuántas relaciones familiares directas, ya sean cosanguíneas o por",
                    "afinidad (matrimonios, parejas de hecho, adopciones), debemos atravesar para llegar de un individuo al otro.");

            float textIndY = textSize * 1.3f;
            textPosY = textPosY + textIndY * 2 + textParagraphSep;

            writeText(stream, light, textSize, 1.3f, textPosX, textPosY,
                    "Las relaciones familiares directas son:");

            float textIndX = 25f;
            textPosY = textPosY + textIndY * 1 + textParagraphSep;

            writeText(stream, mono, 11f, 1.2272f, textPosX + textIndX, textPosY + 0.5f,
                    "•",
                    "•");

            writeText(stream, light, textSize, 1.35f, textPosX + textIndX + 10f, textPosY,
                    "Relación familiar cosanguínea o adoptiva:  padre/madre,  hijo/hija,  (medio-)hermano/a",
                    "Relación familiar por matrimonio o pareja de hecho:  pareja");

            textIndY = textSize * 1.35f;
            textPosY = textPosY + textIndY * 2 + textParagraphSep;

            writeText(stream, light, textSize, 1.3f, textPosX, textPosY,
                    "En otras palabras, nos dice qué tan lejos está una persona de otra en el árbol. Y dado que se calcula tomando",
                    "en cuenta relaciones por afinidad, no implica que dichas personas estén emparentadas.");

            textIndY = textSize * 1.3f;
            float yPos = textPosY + textIndY * 2 + 15f;

            float distancesNoSepY = 240f;

            float size = 8.9f;
            float space = 1.28f;
            float monoSize = 8.3f;
            float monoSpace = (size * space) / monoSize;

            MutableInt index = new MutableInt(0);

            distances
                    .stream()
                    .limit(MAX_DISTINGUISHED_PERSONS_TO_DISPLAY / 2)
                    .forEach(distance -> {
                        int lineIndex = index.getAndIncrement();
                        float lineYPos = yPos + (size * space) * lineIndex;

                        try {
                            if (distance.isRelative()) {
                                writeText(stream, mono, monoSize, monoSpace, 25f, lineYPos - 1f, "ƒ");
                            }
                            writeText(stream, light, size, space, 35f, lineYPos, distance.displayName());
                            String distanceNo = StringUtils.leftPad(String.valueOf(distance.distance()), 2);
                            writeText(stream, light, size, space, 35f + distancesNoSepY, lineYPos, distanceNo);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

            index.setValue(0);

            distances
                    .stream()
                    .skip(MAX_DISTINGUISHED_PERSONS_TO_DISPLAY / 2)
                    .limit(MAX_DISTINGUISHED_PERSONS_TO_DISPLAY)
                    .forEach(distance -> {
                        int lineIndex = index.getAndIncrement();
                        float lineYPos = yPos + (size * space) * lineIndex;

                        try {
                            if (distance.isRelative()) {
                                writeText(stream, mono, monoSize, monoSpace, 300f, lineYPos - 1f, "ƒ");
                            }
                            writeText(stream, light, size, space, 310f, lineYPos, distance.displayName());
                            String distanceNo = StringUtils.leftPad(String.valueOf(distance.distance()), 2);
                            writeText(stream, light, size, space, 310f + distancesNoSepY, lineYPos, distanceNo);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

            writePageNumber(stream, font, pageNum);
            writeGeneratedOn(stream, font);
            writeCollaborate(stream, light);
        }

        return true;
    }

    private boolean writePathToPersonPage(
            PDDocument document,
            List<FormattedShortestPathRelationship> relationships,
            int pageNum,
            PDImageXObject logoImage,
            PDFont font,
            PDFont bold,
            PDFont light) throws IOException {

        if (relationships.isEmpty()) {
            return false;
        }

        PDPage lastPage = new PDPage(PDRectangle.A4);
        document.addPage(lastPage);

        try (PDPageContentStream stream = new PDPageContentStream(document, lastPage)) {

            drawLogoImage(stream, logoImage, A4_MAX_OFFSET_X - 160f, 10f);

            writeText(stream, bold, 16f, 1.2f, 30f, 40f,
                    "Genea Azul");
            writeText(stream, font, 11.5f, 1.2f, 30f, 60f,
                    "sitio web: geneaazul.com.ar  -  en redes sociales: @genea.azul");

            float textPosX = 30f;
            float textPosY = 95f;

            writeText(stream, bold, 12.5f, 1.2f, textPosX, textPosY,
                    "Detalle de ralaciones a:  " + relationships.getLast().displayName());

            float yPos = textPosY + 30f;
            float size = 9.5f;
            float space = getSpace(relationships.size());

            MutableInt index = new MutableInt(0);

            relationships
                    .forEach(relationship -> {
                        try {
                            int lineIndex = index.getAndIncrement();
                            float lineYPos = yPos + (size * space) * lineIndex;

                            String text = (relationship.personInfo() != null)
                                    ? relationship.displayName() + "  " + relationship.personInfo()
                                    : relationship.displayName();
                            writeText(stream, light, size, space, 40f, lineYPos, text);

                            if (relationship.relationshipDesc() != null
                                    && !StringUtils.startsWithIgnoreCase(relationship.relationshipDesc(), "persona principal")) {
                                lineIndex = index.getAndIncrement();
                                lineYPos = yPos + (size * space) * lineIndex;

                                writeText(stream, font, size, space, 55f, lineYPos, relationship.relationshipDesc() + " de");
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

            writePageNumber(stream, font, pageNum);
            writeGeneratedOn(stream, font);

            if (relationships
                    .stream()
                    .anyMatch(FormattedShortestPathRelationship::isObfuscated)) {
                writeReveal(stream, light);
            } else {
                writeCollaborate(stream, light);
            }
        }

        return true;
    }

    private static float getSpace(int elements) {
        float minSpace = 1.15f;
        float maxSpace = 1.55f;
        int maxRelationshipsForMinSpace = 30;
        int maxRelationshipsForMaxSpace = 22;
        return (elements <= maxRelationshipsForMaxSpace)
                ? maxSpace
                : ((elements > maxRelationshipsForMinSpace)
                        ? minSpace
                        : maxSpace - ((float) elements - maxRelationshipsForMaxSpace) / (maxRelationshipsForMinSpace - maxRelationshipsForMaxSpace) * (maxSpace - minSpace));
    }

    private void writeLastPage(
            PDDocument document,
            int pageNum,
            PDImageXObject logoImage,
            PDFont font,
            PDFont bold,
            PDFont light,
            PDFont italic,
            PDFont mono) throws IOException {

        PDPage lastPage = new PDPage(PDRectangle.A4);
        document.addPage(lastPage);

        try (PDPageContentStream stream = new PDPageContentStream(document, lastPage)) {

            drawLogoImage(stream, logoImage, A4_MAX_OFFSET_X - 160f, 10f);

            writeText(stream, bold, 16f, 1.2f, 30f, 40f,
                    "Genea Azul");
            writeText(stream, font, 11.5f, 1.2f, 30f, 60f,
                    "sitio web: geneaazul.com.ar  -  en redes sociales: @genea.azul");

            writeText(stream, bold, 12.5f, 1.2f, 30f, 95f,
                    "Leyenda:");

            float monoSize = 11.5f;
            float monoSpace = 1.25f;
            float textIndY = monoSize * monoSpace;
            float lightSize = 11f;
            float lightSpace = textIndY / lightSize;

            float legendX = 50f;
            float legendY = 115f;
            float legendIndX = 25f;
            float legendSepX = 150f;

            writeText(stream, mono, monoSize, monoSpace, legendX, legendY,
                    "♀",
                    "♂",
                    "✝",
                    "ƒ");
            writeText(stream, light, lightSize, lightSpace, legendX + legendIndX, legendY,
                    "mujer",
                    "varón",
                    "difunto/a",
                    "familiar");

            writeText(stream, mono, monoSize, monoSpace, legendX + legendSepX * 0.8f, legendY,
                    "←",
                    "→",
                    "↔",
                    "◇");
            writeText(stream, light, lightSize, lightSpace, legendX + legendSepX * 0.8f + legendIndX, legendY,
                    "rama paterna",
                    "rama materna",
                    "rama paterna y materna",
                    "rama política (pareja)");

            writeText(stream, mono, monoSize, monoSpace, legendX + 2 * legendSepX, legendY,
                    "↓",
                    "↙",
                    "↘",
                    "⇊");
            writeText(stream, light, lightSize, lightSpace, legendX + 2 * legendSepX + legendIndX, legendY,
                    "rama descendente",
                    "rama descendente y paterna",
                    "rama descendente y materna",
                    "rama descendente, paterna y materna");

            //noinspection DataFlowIssue
            legendX = 50f;
            legendY = legendY + textIndY * 4;
            //noinspection DataFlowIssue
            legendIndX = 25f;
            legendSepX = 85f;

            writeText(stream, text -> "I".equals(text) ? italic : light, lightSize, lightSpace, legendX, legendY,
                    new String[] { null, "~" },
                    new String[] { null, "----" },
                    new String[] { null, "*" },
                    new String[] { null, "<nombre privado>" },
                    new String[] { "I", "relación en cursiva" });
            writeText(stream, light, lightSize, lightSpace, legendX + legendIndX, legendY,
                    "año de nacimiento aproximado",
                    "año de nacimiento de persona viva o cercana a la persona principal",
                    "persona destacada");
            writeText(stream, light, lightSize, lightSpace, legendX + legendIndX + legendSepX, legendY + textIndY * 3,
                    "nombre de persona viva o cercana a la persona principal",
                    "relación familar dada a través de una rama adoptiva");

            legendX = 30f;
            legendY = legendY + textIndY * 5 + 25f;
            legendSepX = 15f;
            float legendSepY = -10f; // text over the image

            // Original image size: 1422 x 1285
            float imageWidth = 500f;
            float imageHeight = 1285f * imageWidth / 1422f;
            float imagePosY = A4_MAX_OFFSET_Y - imageHeight - legendY - legendSepY;

            PDImageXObject relationshipsImage = loadImage(document, "images/relationships-graph.png");
            stream.drawImage(relationshipsImage, legendX + legendSepX, imagePosY, imageWidth, imageHeight);

            // Write text after the image, cause it is overlapped
            writeText(stream, bold, 12.5f, 1.2f, legendX, legendY,
                    "Nomenclatura de relaciones familiares:");

            writePageNumber(stream, font, pageNum);
            writeGeneratedOn(stream, font);
            writeCollaborate(stream, light);
        }
    }

    private Pair<List<FormattedShortestPathDistance>, List<List<FormattedShortestPathRelationship>>> calculateFormattedShortestPath(
            EnrichedPerson person,
            Set<Integer> distinguishedRelatives,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription) {
        long startTime = System.currentTimeMillis();

        var shortestPathFromSource = PathUtils.calculateShortestPathFromSource(
                person.getGedcom(),
                person,
                true,
                true);
        Map<Integer, Integer> shortestDistancesByPersonId = shortestPathFromSource.getLeft();
        Map<Integer, List<Integer>> shortestPathsByPersonId = shortestPathFromSource.getRight();

        List<FormattedShortestPathDistance> formattedShortestPathDistances = person.getGedcom()
                .getPeople()
                .stream()
                .filter(EnrichedPerson::isDistinguishedPerson)
                .filter(distinguished -> !distinguished.getId().equals(person.getId()))
                .map(distinguished -> new FormattedShortestPathDistance(
                        distinguished.getGivenName().map(GivenName::simplified).orElseThrow(),
                        distinguished.getSurname().map(Surname::simplified).orElseThrow(),
                        distinguished.getDisplayName(),
                        distinguishedRelatives.contains(distinguished.getId()),
                        shortestDistancesByPersonId.get(distinguished.getId())))
                .filter(distance -> distance.distance() != null)
                .sorted(Comparator.comparing(FormattedShortestPathDistance::distance)
                        .thenComparing(FormattedShortestPathDistance::isRelative, Comparator.reverseOrder())
                        .thenComparing(FormattedShortestPathDistance::surnameSimplified)
                        .thenComparing(FormattedShortestPathDistance::givenNameSimplified)
                        .thenComparing(FormattedShortestPathDistance::displayName))
                .limit(MAX_DISTINGUISHED_PERSONS_TO_DISPLAY)
                .toList();

        List<List<FormattedShortestPathRelationship>> formattedShortestPathRelationshipsList = SHORTEST_PATH_TARGET_PERSON_IDS
                .stream()
                .map(shortestPathPersonId -> {
                    List<FormattedShortestPathRelationship> formattedShortestPathRelationships = new ArrayList<>();
                    List<Integer> shortestPath = shortestPathsByPersonId.getOrDefault(shortestPathPersonId, List.of());
                    if (shortestPath.size() > 1) {
                        for (int i = 0; i < shortestPath.size(); i++) {
                            EnrichedPerson personA = Objects.requireNonNull(person.getGedcom().getPersonById(shortestPath.get(i)));
                            EnrichedPerson personB = (i < shortestPath.size() - 1) ? Objects.requireNonNull(person.getGedcom().getPersonById(shortestPath.get(i + 1))) : null;
                            Relationship relationship = (i < shortestPath.size() - 1) ? personService.getRelationshipBetween(personB, personA) : Relationship.empty(personA);
                            RelationshipDto relationshipDto = relationshipMapper.toRelationshipDto(
                                    relationship,
                                    getObfuscateCondition(obfuscateLiving, person, personA, i));
                            FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, onlySecondaryDescription);

                            formattedShortestPathRelationships
                                    .add(new FormattedShortestPathRelationship(
                                            formattedRelationship.personName(),
                                            displayPersonInfo(formattedRelationship),
                                            formattedRelationship.relationshipDesc(),
                                            formattedRelationship.isObfuscated()));
                        }
                    }
                    return formattedShortestPathRelationships;
                })
                .toList();

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Shortest paths calculation time: {} ms", totalTime);
        return Pair.of(formattedShortestPathDistances, formattedShortestPathRelationshipsList);
    }

    private @Nullable String displayPersonInfo(FormattedRelationship formattedRelationship) {
        if (formattedRelationship.personYearOfBirth() == null && formattedRelationship.personCountryOfBirth() == null) {
            return null;
        }
        if (formattedRelationship.personYearOfBirth() == null) {
            return "(" + formattedRelationship.personCountryOfBirth() + ")";
        }
        if (formattedRelationship.personCountryOfBirth() == null) {
            return "(" + formattedRelationship.personYearOfBirth() + ")";
        }
        return "(" + formattedRelationship.personYearOfBirth() + ", " + formattedRelationship.personCountryOfBirth() + ")";
    }

    private boolean getObfuscateCondition(boolean obfuscateLiving, EnrichedPerson rootPerson, Relationship relationship) {
        return obfuscateLiving
                // don't obfuscate root person
                && !relationship.person().getId().equals(rootPerson.getId())
                && (rootPerson.isAlive() && relationship.getDistance() <= MAX_DISTANCE_TO_OBFUSCATE || relationship.person().isAlive());
    }

    private boolean getObfuscateCondition(boolean obfuscateLiving, EnrichedPerson rootPerson, EnrichedPerson relPerson, int distance) {
        return obfuscateLiving
                // don't obfuscate root person
                && !relPerson.getId().equals(rootPerson.getId())
                && (rootPerson.isAlive() && distance <= MAX_DISTANCE_TO_OBFUSCATE || relPerson.isAlive());
    }

    @SuppressWarnings("SameParameterValue")
    private void drawLogoImage(PDPageContentStream stream, PDImageXObject image, float x, float y) throws IOException {
        // Original image size: 502 x 264
        float imageWidth = 120f;
        float imageHeight = 264f * imageWidth / 502f;
        float imagePosY = A4_MAX_OFFSET_Y - imageHeight - y;

        stream.drawImage(image, x, imagePosY, imageWidth, imageHeight);
    }

    private void writePeopleInPage(
            PDPageContentStream stream,
            PDFont font,
            PDFont light,
            PDFont italic,
            PDFont mono,
            List<FormattedRelationship> peopleInPage,
            float yPos,
            int pageNum,
            boolean isLastPageOfPeople,
            boolean isAnyPersonObfuscated) throws IOException {

        float size1 = 10.5f;
        float size2 = 9.2f;
        float size3 = 12.5f;
        float size4 = 8.6f;

        float space1 = 1.15f;
        float space2 = size1 * space1 / size2;
        float space3 = size1 * space1 / size3;
        float space4 = size1 * space1 / size4;

        MutableInt index = new MutableInt(0);

        peopleInPage.forEach(relationship -> {
            int lineIndex = index.getAndIncrement();
            float lineYPos1 = yPos + (size1 * space1) * lineIndex;
            float lineYPos2 = yPos + (size2 * space2) * lineIndex;
            float lineYPos3 = yPos + (size3 * space3) * lineIndex;
            float lineYPos4 = yPos + (size4 * space4) * lineIndex;

            String text1 = StringUtils.leftPad(relationship.index(), 4)
                    + ". " + relationship.personSex()
                    + " " + relationship.treeSide()
                    + " " + relationship.personIsAlive();

            String countryForReport = PlaceUtils.adjustCountryForReport(relationship.personCountryOfBirth());
            String text2 = rightPadFixedWidth(countryForReport, 3);

            int padding = "----".equals(relationship.personYearOfBirth())
                    ? 9
                    : (StringUtils.startsWith(relationship.personYearOfBirth(), "~") ? 5 : 7);
            String text3 = leftPadFixedWidth(relationship.personYearOfBirth(), padding);

            String text4 = StringUtils.substring(relationship.personName(), 0, MAX_PERSON_NAME_LENGTH);

            String text5 = relationship.distinguishedPerson();

            String text6 = "• ";

            String[] text7 = { relationship.adoption(), relationship.relationshipDesc() };

            try {
                writeText(stream, mono, size1, space1, 30f, lineYPos1, text1);
                writeText(stream, light, size2, space2, 102f, lineYPos2, text2);
                writeText(stream, light, size2, space2, 120f, lineYPos2, text3);
                writeText(stream, light, size2, space2, 155f, lineYPos2, text4);
                writeText(stream, mono, size3, space3, 360f, lineYPos3, text5);
                writeText(stream, light, size2, space2, 370f, lineYPos2, text6);
                writeText(stream, text -> StringUtils.isNotBlank(text) ? italic : light, size4, space4, 375f, lineYPos4, text7);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        writePageNumber(stream, font, pageNum);
        if (isLastPageOfPeople) {
            writeGeneratedOn(stream, font);
        }

        if (isAnyPersonObfuscated) {
            writeReveal(stream, light);
        } else {
            writeCollaborate(stream, light);
        }
    }

    private void writeText(
            PDPageContentStream stream,
            PDFont font,
            float size,
            float space,
            float x,
            float y,
            String text) throws IOException {
        if (StringUtils.isNotBlank(text)) {
            stream.beginText();
            stream.setFont(font, size);
            stream.setLeading(size * space);
            stream.newLineAtOffset(x, A4_MAX_OFFSET_Y - y);
            stream.showText(text);
            stream.endText();
        }
    }

    private void writeText(
            PDPageContentStream stream,
            PDFont font,
            float size,
            float space,
            float x,
            float y,
            String... texts) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.setLeading(size * space);
        stream.newLineAtOffset(x, A4_MAX_OFFSET_Y - y);
        for (String text : texts) {
            stream.showText(text);
            stream.newLine();
        }
        stream.endText();
    }

    private void writeText(
            PDPageContentStream stream,
            Function<String, PDFont> fontResolver,
            float size,
            float space,
            @SuppressWarnings("SameParameterValue") float x,
            float y,
            String[] text) throws IOException {
        stream.beginText();
        stream.setLeading(size * space);
        stream.newLineAtOffset(x, A4_MAX_OFFSET_Y - y);
        stream.setFont(fontResolver.apply(text[0]), size);
        stream.showText(text[1]);
        stream.endText();
    }

    @SuppressWarnings("SameParameterValue")
    private void writeText(
            PDPageContentStream stream,
            Function<String, PDFont> fontResolver,
            float size,
            float space,
            @SuppressWarnings("SameParameterValue") float x,
            float y,
            String[]... texts) throws IOException {
        stream.beginText();
        stream.setLeading(size * space);
        stream.newLineAtOffset(x, A4_MAX_OFFSET_Y - y);
        for (String[] text : texts) {
            stream.setFont(fontResolver.apply(text[0]), size);
            stream.showText(text[1]);
            stream.newLine();
        }
        stream.endText();
    }

    private void writePageNumber(PDPageContentStream stream, PDFont font, int pageNum) throws IOException {
        writeText(stream, font, 12f, 1.2f, 500f, 780f, "Página " + pageNum);
    }

    private void writeGeneratedOn(PDPageContentStream stream, PDFont font) throws IOException {
        String dateTimeStr = ZonedDateTime
                .now(properties.getZoneId())
                .format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN, properties.getLocale()));
        dateTimeStr = StringUtils.replace(dateTimeStr, "\u202F", " ");
        float offset = (12f - 11.2f) * 1.2f;
        writeText(stream, font, 11.5f, 1.2f, 30f, 780f + offset,
                "Generado el " + dateTimeStr);
    }

    private void writeReveal(PDPageContentStream stream, PDFont font) throws IOException {
        writeText(stream, font, 10.f, 1.2f, 80f, 805f,
                "Para revelar las datos privados de las personas ponete en contacto con nosotros (no tiene costo).");
    }

    private void writeCollaborate(PDPageContentStream stream, PDFont font) throws IOException {
        writeText(stream, font, 10f, 1.2f, 155f, 805f,
                "¡Colaborá con este proyecto! ¡Solicitá acceso al árbol y cargá info!");
    }

    @SuppressWarnings("SameParameterValue")
    private PDFont loadFont(PDDocument document, EmbeddedFontsConfig.Font font) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(embeddedFonts.get(font));
        return PDType0Font.load(document, resource.getInputStream());
    }

    @SuppressWarnings("SameParameterValue")
    private PDImageXObject loadImage(PDDocument document, String imagePath) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(imagePath);
        return PDImageXObject.createFromByteArray(document, resource.getContentAsByteArray(), imagePath);
    }

    private static String leftPadFixedWidth(String value, @SuppressWarnings("SameParameterValue") int width) {
        value = StringUtils.defaultString(value);
        return StringUtils.leftPad(StringUtils.substring(value, 0, width), width);
    }

    private static String rightPadFixedWidth(String value, @SuppressWarnings("SameParameterValue") int width) {
        value = StringUtils.defaultString(value);
        return StringUtils.rightPad(StringUtils.substring(value, 0, width), width);
    }

}
