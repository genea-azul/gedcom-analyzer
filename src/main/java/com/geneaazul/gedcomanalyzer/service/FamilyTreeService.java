package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.EmbeddedFontsConfig;
import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
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
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
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

    public void exportToPDF(Path path, String personName, List<FormattedRelationship> peopleInTree) throws IOException {

        // Create a document
        try (PDDocument document = new PDDocument()) {

            PDFont font = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO);
            PDFont bold = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO_BOLD);
            PDFont light = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO_LIGHT);
            PDFont mono = loadFont(document, EmbeddedFontsConfig.Font.EVERSON_MONO);

            int maxIndexWidth = peopleInTree.get(peopleInTree.size() - 1).index().length();
            int maxPersonsInFirstPage = 39;
            int maxPersonsInNextPages = 67;

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
                writeText(stream, light, 11.5f, 1.2f, 50f, 261f,
                        "<nombre privado>");
                writeText(stream, light, 11.5f, 1.2f, 160f, 261f,
                        "nombre de persona viva o cercana a la persona principal");

                writeText(stream, bold, 13f, 1.2f, 30f, 300f,
                        "Árbol genealógico de " + personName);
                writeText(stream, light, 11.5f, 1.2f, 50f, 320f,
                        "Personas: " + peopleInTree.size());

                writePeopleInPage(
                        stream,
                        font,
                        light,
                        mono,
                        peopleInTree.subList(0, Math.min(peopleInTree.size(), maxPersonsInFirstPage)),
                        350f,
                        maxIndexWidth,
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
                    writeText(stream, bold, 13f, 1.2f, 30f, 40f, "Árbol genealógico de " + personName);

                    writePeopleInPage(
                            stream,
                            font,
                            light,
                            mono,
                            peopleInPage,
                            60f,
                            maxIndexWidth,
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
            int maxIndexWidth,
            int pageNum,
            boolean isLastPage) throws IOException {

        List<String> lines = peopleInPage
                .stream()
                .map(relationship
                        -> StringUtils.leftPad(relationship.index(), maxIndexWidth)
                        + ". " + relationship.personSex()
                        + " " + relationship.treeSide()
                        + " " + relationship.personIsAlive()
                        + " " + rightPadFixedWidth(PlaceUtils.adjustCountryForReport(relationship.personCountry()), 3))
                .toList();
        writeText(stream, mono, 10.5f, 1f, 30f, yPos, lines.toArray(String[]::new));

        lines = peopleInPage
                .stream()
                .map(FormattedRelationship::personName)
                .map(name -> StringUtils.substring(name, 0, 42))
                .toList();
        writeText(stream, light, 10.5f, 1f, 135f, yPos, lines.toArray(String[]::new));

        lines = peopleInPage
                .stream()
                .map(relationship -> "• " + relationship.relationshipDesc())
                .toList();
        writeText(stream, light, 10.5f, 1f, 345f, yPos, lines.toArray(String[]::new));

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

    @SuppressWarnings({"DataFlowIssue", "SameParameterValue"})
    private PDFont loadFont(PDDocument document, EmbeddedFontsConfig.Font font) throws IOException {
        File file = new File(getClass().getResource(embeddedFonts.get(font)).getFile());
        return PDType0Font.load(document, file);
    }

    private static String rightPadFixedWidth(String value, @SuppressWarnings("SameParameterValue") int width) {
        value = StringUtils.defaultString(value);
        return StringUtils.rightPad(StringUtils.substring(value, 0, width), width);
    }

}