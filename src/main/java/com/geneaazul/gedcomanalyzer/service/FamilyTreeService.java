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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
            PDFont italic = loadFont(document, EmbeddedFontsConfig.Font.ROBOTO_LIGHT_ITALIC);
            PDFont mono = loadFont(document, EmbeddedFontsConfig.Font.EVERSON_MONO);

            int maxPersonsInFirstPage = 35;
            int maxPersonsInNextPages = 59;

            boolean isAnyPersonObfuscated = peopleInTree
                    .stream()
                    .anyMatch(FormattedRelationship::isObfuscated);

            PDPage firstPage = new PDPage(PDRectangle.A4);
            document.addPage(firstPage);

            try (PDPageContentStream stream = new PDPageContentStream(document, firstPage)) {
                writeText(stream, bold, 16f, 1.2f, 30f, 40f,
                        "Genea Azul - Estudio de Genealogía Azuleña");
                writeText(stream, font, 11.5f, 1.2f, 30f, 60f,
                        "geneaazul.com.ar  -  en redes sociales: @genea.azul");

                writeText(stream, bold, 12.5f, 1.2f, 30f, 95f,
                        "Leyenda:");

                float legendX = 50f;
                float legendY = 115f;
                float legendIndX = 25f;
                float legendSepX = 150f;

                writeText(stream, mono, 11.5f, 1.175f, legendX, legendY,
                        "♀",
                        "♂",
                        "✝");
                writeText(stream, light, 11f, 1.22f, legendX + legendIndX, legendY,
                        "mujer",
                        "varón",
                        "difunto/a");

                writeText(stream, mono, 11.5f, 1.175f, legendX + legendSepX * 0.8f, legendY,
                        "←",
                        "→",
                        "↔",
                        "◇");
                writeText(stream, light, 11f, 1.22f, legendX + legendSepX * 0.8f + legendIndX, legendY,
                        "rama paterna",
                        "rama materna",
                        "rama paterna y materna",
                        "rama política (pareja)");

                writeText(stream, mono, 11.5f, 1.175f, legendX + 2 * legendSepX, legendY,
                        "↓",
                        "↙",
                        "↘",
                        "⇊");
                writeText(stream, light, 11f, 1.22f, legendX + 2 * legendSepX + legendIndX, legendY,
                        "rama descendente",
                        "rama descendente y paterna",
                        "rama descendente y materna",
                        "rama descendente, paterna y materna");

                //noinspection DataFlowIssue
                legendX = 50f;
                legendY = 168f;
                //noinspection DataFlowIssue
                legendIndX = 25f;
                legendSepX = 85f;
                float legendSepY = 39.6f;

                writeText(stream, text -> "I".equals(text) ? italic : light, 11f, 1.2f, legendX, legendY,
                        new String[] { null, "~" },
                        new String[] { null, "----" },
                        new String[] { null, "*" },
                        new String[] { null, "<nombre privado>" },
                        new String[] { "I", "relación en cursiva" });
                writeText(stream, light, 11f, 1.2f, legendX + legendIndX, legendY,
                        "año de nacimiento aproximado",
                        "año de nacimiento de persona viva o cercana a la persona principal",
                        "persona destacada");
                writeText(stream, light, 11f, 1.2f, legendX + legendIndX + legendSepX, legendY + legendSepY,
                        "nombre de persona viva o cercana a la persona principal",
                        "relación familar dada a través de una rama adoptiva");

                legendX = 30f;
                legendY = 258.2f;

                writeText(stream, bold, 12.5f, 1.2f, legendX, legendY,
                        "Árbol genealógico de " + person.getDisplayName());

                legendX = legendX + 20f;
                legendY = legendY + 20f;
                legendSepY = 70f;

                writeText(stream, light, 11f, 1.3f, legendX, legendY,
                        "Personas: " + person.getPersonsCountInTree(),
                        "Apellidos (en caso de apellidos compuestos sólo se considera el primero): " + person.getSurnamesCountInTree(),
                        "Generaciones: " + person.getAncestryGenerations().getTotalGenerations()
                                + " (ascendencia: " + person.getAncestryGenerations().ascending()
                                + ", descendencia: " + person.getAncestryGenerations().directDescending() + ")",
                        "Países en su ascendencia: " + (person.getAncestryCountries().isEmpty() ? "-" : String.join(", ", person.getAncestryCountries())));

                writePeopleInPage(
                        stream,
                        font,
                        light,
                        italic,
                        mono,
                        peopleInTree.subList(0, Math.min(peopleInTree.size(), maxPersonsInFirstPage)),
                        legendY + legendSepY,
                        1,
                        peopleInTree.size() <= maxPersonsInFirstPage,
                        isAnyPersonObfuscated);
            }

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

            // Save the document
            Files.createDirectories(path.getParent());
            document.save(path.toFile());
        }

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
            boolean isLastPage,
            boolean isAnyPersonObfuscated) throws IOException {

        float size1 = 10.5f;
        float size2 = 9.2f;
        float size3 = 12.5f;
        float size4 = 8.6f;

        float space1 = 1.15f;
        float space2 = size1 * space1 / size2;
        float space3 = size1 * space1 / size3;
        float space4 = size1 * space1 / size4;

        String[] lines = peopleInPage
                .stream()
                .map(relationship
                        -> StringUtils.leftPad(relationship.index(), 4)
                        + ". " + relationship.personSex()
                        + " " + relationship.treeSide()
                        + " " + relationship.personIsAlive())
                .toArray(String[]::new);
        writeText(stream, mono, size1, space1, 30f, yPos, lines);

        lines = peopleInPage
                .stream()
                .map(relationship -> rightPadFixedWidth(PlaceUtils.adjustCountryForReport(relationship.personCountryOfBirth()), 3))
                .toArray(String[]::new);
        writeText(stream, light, size2, space2, 102f, yPos, lines);

        lines = peopleInPage
                .stream()
                .map(relationship -> {
                    int padding = "----".equals(relationship.personYearOfBirth())
                            ? 9
                            : (StringUtils.startsWith(relationship.personYearOfBirth(), "~") ? 5 : 7);
                    return leftPadFixedWidth(relationship.personYearOfBirth(), padding);
                })
                .toArray(String[]::new);
        writeText(stream, light, size2, space2, 120f, yPos, lines);

        lines = peopleInPage
                .stream()
                .map(FormattedRelationship::personName)
                .map(name -> StringUtils.substring(name, 0, 43))
                .toArray(String[]::new);
        writeText(stream, light, size1, space1, 155f, yPos, lines);

        lines = peopleInPage
                .stream()
                .map(FormattedRelationship::distinguishedPerson)
                .toArray(String[]::new);
        writeText(stream, mono, size3, space3, 360f, yPos, lines);

        lines = peopleInPage
                .stream()
                .map(relationship -> "• ")
                .toArray(String[]::new);
        writeText(stream, light, size2, space2, 370f, yPos, lines);

        String[][] linesWithCond = peopleInPage
                .stream()
                .map(relationship -> new String[] { relationship.adoption(), relationship.relationshipDesc() })
                .toArray(String[][]::new);
        writeText(stream, text -> StringUtils.isNotBlank(text) ? italic : light, size4, space4, 375f, yPos, linesWithCond);

        writeText(stream, font, 12f, 1.2f, 500f, 780f, "Página " + pageNum);

        if (isLastPage) {
            writeText(stream, font, 12f, 1.2f, 30f, 780f,
                    "Generado el " + ZonedDateTime
                            .now(properties.getZoneId())
                            .format(DateTimeFormatter
                                    .ofLocalizedDateTime(FormatStyle.FULL)
                                    .localizedBy(properties.getLocale())));
        }

        if (isAnyPersonObfuscated) {
            writeText(stream, light, 10.f, 1.2f, 75f, 805f,
                    "Para revelar las datos privados de las personas ponete en contacto con nosotros (no tiene costo).");
        }
    }

    private void writeText(PDPageContentStream stream, PDFont font, float size, float space, float x, float y, String... texts) throws IOException {
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
