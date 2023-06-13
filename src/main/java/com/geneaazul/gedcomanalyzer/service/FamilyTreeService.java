package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.EmbeddedFontsConfig;
import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.utils.PlaceUtils;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyTreeService {

    @SuppressWarnings("unused")
    private static final float A4_MAX_OFFSET_X = 590f;
    private static final float A4_MAX_OFFSET_Y = 830f;

    private final GedcomAnalyzerProperties properties;
    private final Map<EmbeddedFontsConfig.Font, String> embeddedFonts;

    public void exportToPDF(Path path, EnrichedPerson person, List<FormattedRelationship> peopleInTree) throws IOException {

        // Create a document
        try (PDDocument document = new PDDocument()) {

            PDFont font = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO);
            PDFont bold = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO_BOLD);
            PDFont light = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO_LIGHT);
            PDFont mono = loadFont(document, EmbeddedFontsConfig.Font.EVERSON_MONO);

            int maxPersonsInFirstPage = 30;
            int maxPersonsInNextPages = 59;

            PDPage firstPage = new PDPage(PDRectangle.A4);
            document.addPage(firstPage);

            try (PDPageContentStream stream = new PDPageContentStream(document, firstPage)) {
                writeText(stream, bold, 16f, 1.2f, 30f, 40f,
                        "Genea Azul - Estudio de Genealogía Azuleña");
                writeText(stream, font, 12f, 1.2f, 30f, 60f,
                        "geneaazul.com.ar  -  en redes sociales: @genea.azul");

                writeText(stream, bold, 13f, 1.2f, 30f, 100f,
                        "Leyenda:");
                writeText(stream, mono, 11.5f, 1.23f, 50f, 120f,
                        "♀",
                        "♂",
                        "✝",
                        "←",
                        "→",
                        "↔",
                        "↓",
                        "↙",
                        "↘",
                        "⇊");
                writeText(stream, light, 11.5f, 1.2f, 50f, 261f,
                        "~",
                        "----",
                        "<nombre privado>");

                writeText(stream, light, 11.5f, 1.22f, 75f, 120f,
                        "mujer",
                        "varón",
                        "difunto/a",
                        "rama paterna",
                        "rama materna",
                        "rama paterna y materna",
                        "rama descendente",
                        "rama descendente y paterna",
                        "rama descendente y materna",
                        "rama descendente, paterna y materna");
                writeText(stream, light, 11.5f, 1.2f, 75f, 261f,
                        "año de nacimiento aproximado",
                        "año de nacimiento de persona viva o cercana a la persona principal");
                writeText(stream, light, 11.5f, 1.2f, 160f, 288.5f,
                        "nombre de persona viva o cercana a la persona principal");

                writeText(stream, bold, 13f, 1.2f, 30f, 330f,
                        "Árbol genealógico de " + person.getDisplayName());
                writeText(stream, light, 11.5f, 1.2f, 50f, 350f,
                        "Personas: " + person.getNumberOfPeopleInTree(),
                        "Generaciones: " + person.getAncestryGenerations().getTotalGenerations()
                                + " (ascendencia: " + person.getAncestryGenerations().ascending()
                                + ", descendencia: " + person.getAncestryGenerations().directDescending() + ")",
                        "Países en su ascendencia: " + (person.getAncestryCountries().isEmpty() ? "-" : String.join(", ", person.getAncestryCountries())));

                writePeopleInPage(
                        stream,
                        font,
                        light,
                        mono,
                        peopleInTree.subList(0, Math.min(peopleInTree.size(), maxPersonsInFirstPage)),
                        410f,
                        1,
                        peopleInTree.size() <= maxPersonsInFirstPage);
            }

            List<List<FormattedRelationship>> nextPages = (peopleInTree.size() > maxPersonsInFirstPage)
                    ? ListUtils.partition(peopleInTree.subList(maxPersonsInFirstPage, peopleInTree.size()), maxPersonsInNextPages)
                    : List.of();

            for (int i = 0; i < nextPages.size(); i++) {
                List<FormattedRelationship> peopleInPage = nextPages.get(i);
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    writeText(stream, bold, 13f, 1.2f, 30f, 40f, "Árbol genealógico de " + person.getDisplayName());

                    writePeopleInPage(
                            stream,
                            font,
                            light,
                            mono,
                            peopleInPage,
                            60f,
                            i + 2,
                            i == nextPages.size() - 1);
                }
            }

            // Save the document
            Files.createDirectories(path.getParent());
            document.save(path.toFile());
        }

    }

    private void writePeopleInPage(
            PDPageContentStream stream,
            PDFont font,
            PDFont light,
            PDFont mono,
            List<FormattedRelationship> peopleInPage,
            float yPos,
            int pageNum,
            boolean isLastPage) throws IOException {

        float size1 = 10.5f;
        float size2 = 9.2f;

        float space1 = 1.15f;
        float space2 = size1 * space1 / size2;

        List<String> lines = peopleInPage
                .stream()
                .map(relationship
                        -> StringUtils.leftPad(relationship.index(), 4)
                        + ". " + relationship.personSex()
                        + " " + relationship.treeSide()
                        + " " + relationship.personIsAlive())
                .toList();
        writeText(stream, mono, size1, space1, 30f, yPos, lines.toArray(String[]::new));

        lines = peopleInPage
                .stream()
                .map(relationship -> rightPadFixedWidth(PlaceUtils.adjustCountryForReport(relationship.personCountryOfBirth()), 3))
                .toList();
        writeText(stream, light, size2, space2, 102f, yPos, lines.toArray(String[]::new));

        lines = peopleInPage
                .stream()
                .map(relationship -> {
                    int padding = "----".equals(relationship.personYearOfBirth())
                            ? 9
                            : (StringUtils.startsWith(relationship.personYearOfBirth(), "~") ? 5 : 7);
                    return leftPadFixedWidth(relationship.personYearOfBirth(), padding);
                })
                .toList();
        writeText(stream, light, size2, space2, 120f, yPos, lines.toArray(String[]::new));

        lines = peopleInPage
                .stream()
                .map(FormattedRelationship::personName)
                .map(name -> StringUtils.substring(name, 0, 42))
                .toList();
        writeText(stream, light, size1, space1, 155f, yPos, lines.toArray(String[]::new));

        lines = peopleInPage
                .stream()
                .map(relationship -> "• " + relationship.relationshipDesc())
                .toList();
        writeText(stream, light, size2, space2, 370f, yPos, lines.toArray(String[]::new));

        writeText(stream, font, 12f, 1.2f, 500f, 780f, "Página " + pageNum);

        if (isLastPage) {
            writeText(stream, font, 12f, 1.2f, 30f, 780f,
                    "Generado el " + ZonedDateTime
                            .now(properties.getZoneId())
                            .format(DateTimeFormatter
                                    .ofLocalizedDateTime(FormatStyle.FULL)
                                    .localizedBy(properties.getLocale())));
        }
    }

    private void writeText(PDPageContentStream stream, PDFont font, float size, float space, float x, float y, String... texts) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.setLeading(size * space);
        stream.setStrokingColor(Color.RED);
        stream.newLineAtOffset(x, A4_MAX_OFFSET_Y - y);
        for (String text : texts) {
            stream.showText(text);
            stream.newLine();
        }
        stream.endText();
    }

    @SuppressWarnings("SameParameterValue")
    private PDFont loadFont(PDDocument document, EmbeddedFontsConfig.Font font) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(embeddedFonts.get(font));
        return PDType0Font.load(document, resource.getInputStream());
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
