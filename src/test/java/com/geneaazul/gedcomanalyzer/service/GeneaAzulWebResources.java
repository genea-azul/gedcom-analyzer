package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.NameUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.Name;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class GeneaAzulWebResources {

    public static final int MAX_SURNAMES_PER_COUNTRY = 22;

    @Autowired
    private GedcomHolder gedcomHolder;
    @Autowired
    private GedcomAnalyzerService gedcomAnalyzerService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private SurnameService surnameService;

    private EnrichedGedcom gedcom;

    @BeforeEach
    public void setUp() {
        gedcom = gedcomHolder.getGedcom();
    }

    // ── ISO-2 code → flag emoji (regional indicator letters) ──────────
    private static String toFlagEmoji(@Nullable String isoCode) {
        if (isoCode == null) return "🌎";
        return switch (isoCode) {
            case "GB-ENG" -> "🏴󠁧󠁢󠁥󠁮󠁧󠁿";
            case "GB-SCT" -> "🏴󠁧󠁢󠁳󠁣󠁴󠁿";
            case "GB-NIR" -> "🏴󠁧󠁢󠁮󠁩󠁲󠁿";
            default -> isoCode.chars()
                    .mapToObj(c -> new String(Character.toChars(0x1F1E6 + c - 'A')))
                    .collect(Collectors.joining());
        };
    }

    // ── Known country → ISO-2 code (null for non-country entries) ─────
    private static final Map<String, String> COUNTRY_ISO = new LinkedHashMap<>();
    static {
        COUNTRY_ISO.put("Italia",               "IT");
        COUNTRY_ISO.put("España",               "ES");
        COUNTRY_ISO.put("Francia",              "FR");
        COUNTRY_ISO.put("Uruguay",              "UY");
        COUNTRY_ISO.put("Chile",                "CL");
        COUNTRY_ISO.put("Países Bajos",         "NL");
        COUNTRY_ISO.put("Suiza",                "CH");
        COUNTRY_ISO.put("Paraguay",             "PY");
        COUNTRY_ISO.put("Portugal",             "PT");
        COUNTRY_ISO.put("Brasil",               "BR");
        COUNTRY_ISO.put("Estados Unidos",       "US");
        COUNTRY_ISO.put("Irlanda",              "IE");
        COUNTRY_ISO.put("Austria",              "AT");
        COUNTRY_ISO.put("Escocia",              "GB-SCT");
        COUNTRY_ISO.put("Perú",                 "PE");
        COUNTRY_ISO.put("Polonia",              "PL");
        COUNTRY_ISO.put("Japón",                "JP");
        COUNTRY_ISO.put("Ucrania",              "UA");
        COUNTRY_ISO.put("Bélgica",              "BE");
        COUNTRY_ISO.put("Dinamarca",            "DK");
        COUNTRY_ISO.put("Marruecos",            "MA");
        COUNTRY_ISO.put("Bolivia",              "BO");
        COUNTRY_ISO.put("Venezuela",            "VE");
        COUNTRY_ISO.put("Ecuador",              "EC");
        COUNTRY_ISO.put("Océano Atlántico",     null);
        COUNTRY_ISO.put("Australia",            "AU");
        COUNTRY_ISO.put("Bulgaria",             "BG");
        COUNTRY_ISO.put("China",                "CN");
        COUNTRY_ISO.put("Cuba",                 "CU");
        COUNTRY_ISO.put("Irlanda del Norte",    "GB-NIR");
        COUNTRY_ISO.put("Jamaica",              "JM");
        COUNTRY_ISO.put("Nicaragua",            "NI");
        COUNTRY_ISO.put("República Dominicana", "DO");
        COUNTRY_ISO.put("Rumania",              "RO");
    }

    @Test
    public void generateSurnamesJson() throws IOException {
        List<GedcomAnalyzerService.SurnamesCardinality> cardinalities = gedcomAnalyzerService
                .getSurnamesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", null, true, false);

        // Build a lookup map: normalizedMainWord → alive count
        Map<String, Integer> aliveCountByNormalized = gedcomAnalyzerService
                .getSurnamesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", true, true, false)
                .stream()
                .collect(Collectors.toMap(
                        c -> c.mainSurname().normalizedMainWord(),
                        GedcomAnalyzerService.SurnamesCardinality::value));

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < cardinalities.size(); i++) {
            GedcomAnalyzerService.SurnamesCardinality cardinality = cardinalities.get(i);

            List<String> variants = surnameService.getSurnameVariants(
                    cardinality.mainSurname(),
                    cardinality.variantsCardinality().stream().map(Pair::getLeft).toList(),
                    gedcom.getProperties().getNormalizedSurnamesMap());

            String surnameJson = cardinality.mainSurname().value().replace("\"", "\\\"");
            String variantsJson = variants.stream()
                    .map(v -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                    .collect(Collectors.joining(", ", "[", "]"));

            int aliveCount = aliveCountByNormalized.getOrDefault(cardinality.mainSurname().normalizedMainWord(), 0);

            sb.append(String.format(
                    "  {\"surname\": \"%s\", \"count\": %d, \"aliveCount\": %d, \"variants\": %s}%s\n",
                    surnameJson, cardinality.value(), aliveCount, variantsJson,
                    i < cardinalities.size() - 1 ? "," : ""));
        }
        sb.append("]");

        Path output = Path.of("../geneaazul-web/data/surnames.json");
        Files.writeString(output, sb);
        System.out.println("Written: " + output.toAbsolutePath());
    }

    @Test
    public void generateImmigrationJson() throws IOException {

        List<GedcomAnalyzerService.SurnamesByCityCardinality> places = gedcomAnalyzerService
                .getImmigrantsCitiesCardinalityByPlaceOfAnyEvent(
                        gedcom.getPeople(),
                        "Azul, Buenos Aires, Argentina",
                        null,
                        new String[] { "Uruguay", "Brasil", "Chile", "Perú", "Paraguay", "Bolívia", "Océano Atlántico" },
                        null,
                        // includeSpousePlaces: relates to placeOfAnyEvent, set true for wider range of immigrants
                        true,
                        // includeAllChildrenPlaces: relates to placeOfAnyEvent, set true for wider range of immigrants
                        true,
                        // isExactPlace: relates to placeOfAnyEvent, set true to match exactly instead of "ends with" matching
                        false,
                        false,
                        GedcomAnalyzerService.PlacePart.COUNTRY);

        // ── Country groupings ──────────────────────────────────────────
        // Maps each raw GEDCOM country name to its canonical display name.
        // Countries not listed here are used as-is.
        record GroupDef(String displayName, @Nullable String formerly, @Nullable String isoCode) {}

        Map<String, GroupDef> groups = new LinkedHashMap<>();
        GroupDef alemaniaRusia  = new GroupDef("Alemania / Rusia",             null,             "DE");
        GroupDef inglaterra     = new GroupDef("Inglaterra",                   null,             "GB-ENG");
        GroupDef siriaLibano    = new GroupDef("Siria / Líbano",               null,             "SY");
        GroupDef yugoslavia     = new GroupDef("Croacia / Eslovenia / Serbia", "Yugoslavia",     "HR");
        GroupDef checoslovaquia = new GroupDef("República Checa / Eslovaquia", "Checoslovaquia", "CZ");

        groups.put("Alemania",         alemaniaRusia);
        groups.put("Rusia",            alemaniaRusia);
        groups.put("Inglaterra",       inglaterra);
        groups.put("Reino Unido",      inglaterra);
        groups.put("Siria",            siriaLibano);
        groups.put("Líbano",           siriaLibano);
        groups.put("Líbano o Siria",   siriaLibano);
        groups.put("Siria o Líbano",   siriaLibano);
        groups.put("Croacia",          yugoslavia);
        groups.put("Eslovenia",        yugoslavia);
        groups.put("Serbia",           yugoslavia);
        groups.put("Yugoslavia",       yugoslavia);
        groups.put("República Checa",  checoslovaquia);
        groups.put("Eslovaquia",       checoslovaquia);
        groups.put("Checoslovaquia",   checoslovaquia);

        // ── Aggregate count and surnames by group ──────────────────────
        Map<String, Integer> countByGroup      = new LinkedHashMap<>();
        Map<String, String>  formerlyByGroup   = new LinkedHashMap<>();
        Map<String, String>  isoByGroup        = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> surnamesByGroup = new LinkedHashMap<>();

        for (GedcomAnalyzerService.SurnamesByCityCardinality place : places) {
            GroupDef def = groups.get(place.country());
            String key      = def != null ? def.displayName() : place.country();
            String formerly = def != null ? def.formerly()    : null;
            String iso      = def != null ? def.isoCode()     : COUNTRY_ISO.get(place.country());

            countByGroup.merge(key, place.cardinality(), Integer::sum);
            formerlyByGroup.putIfAbsent(key, formerly);
            isoByGroup.putIfAbsent(key, iso);

            Map<String, Integer> surnames = surnamesByGroup.computeIfAbsent(key, k -> new LinkedHashMap<>());
            place.surnames().forEach(t -> surnames.merge(t.getLeft(), t.getMiddle(), Integer::sum));
        }

        int total = countByGroup.values().stream().mapToInt(Integer::intValue).sum();

        // ── Sort by count descending and emit JSON ─────────────────────
        List<Map.Entry<String, Integer>> sorted = countByGroup.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < sorted.size(); i++) {
            String key   = sorted.get(i).getKey();
            int    count = sorted.get(i).getValue();
            float  pct   = (float) Math.round((float) count / total * 10000f) / 100f;

            String iso      = isoByGroup.get(key);
            String formerly = formerlyByGroup.get(key);
            String flag     = toFlagEmoji(iso);

            List<String> topSurnames = surnamesByGroup.getOrDefault(key, Map.of()).entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(MAX_SURNAMES_PER_COUNTRY)
                    .map(Map.Entry::getKey)
                    .toList();

            String formerlyJson  = formerly != null ? "\"" + formerly + "\"" : "null";
            String isoJson       = iso      != null ? "\"" + iso + "\""      : "null";
            String surnamesJson  = topSurnames.stream()
                    .map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                    .collect(Collectors.joining(", ", "[", "]"));

            sb.append(String.format(
                    "  { \"country\": \"%s\", \"formerly\": %s, \"flag\": \"%s\", \"isoCode\": %s, \"count\": %d, \"percentage\": %.2f, \"topSurnames\": %s }%s\n",
                    key.replace("\"", "\\\""), formerlyJson, flag, isoJson, count, pct, surnamesJson,
                    i < sorted.size() - 1 ? "," : ""));
        }
        sb.append("]");

        Path output = Path.of("../geneaazul-web/data/immigration.json");
        Files.writeString(output, sb);
        System.out.println("Written: " + output.toAbsolutePath());
    }

    @Test
    public void generatePersonalitiesJson() throws IOException {
        Map<String, String> namePrefixesMap = gedcom.getProperties().getNamePrefixesMap();

        List<EnrichedPerson> personalities = gedcom.getPeople()
                .stream()
                .filter(EnrichedPerson::isDistinguishedPerson)
                .sorted(Comparator
                        .<EnrichedPerson, String>comparing(
                                p -> p.getSurname().map(Surname::simplified).orElse(null),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(
                                p -> p.getGivenName().map(GivenName::simplified).orElse(null),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < personalities.size(); i++) {
            EnrichedPerson ep = personalities.get(i);

            // ── Name parts from legacy Person ──────────────────────────
            String title = null, titleFull = null, givenName = null, surname = null, nickname = null;
            Name name = ep.getLegacyPerson()
                    .filter(lp -> !lp.getNames().isEmpty())
                    .map(lp -> lp.getNames().get(0))
                    .orElse(null);
            if (name != null) {
                title = StringUtils.trimToNull(name.getPrefix());
                if (title != null) {
                    titleFull = namePrefixesMap.getOrDefault(NameUtils.simplifyName(title), title);
                }
                givenName = StringUtils.trimToNull(name.getGiven());
                nickname  = StringUtils.trimToNull(name.getNickname());
                String sp = StringUtils.trimToNull(name.getSurnamePrefix());
                String s  = StringUtils.trimToNull(name.getSurname());
                surname   = sp != null && s != null ? sp + " " + s : (s != null ? s : sp);
            }

            // ── Dates ───────────────────────────────────────────────────
            String birthYear = ep.getDateOfBirth()
                    .map(d -> (d.getOperator() == Date.Operator.ABT || d.getOperator() == Date.Operator.EST)
                            ? "aprox. " + d.getYear().getValue()
                            : String.valueOf(d.getYear().getValue()))
                    .orElse(null);
            String deathYear = ep.isAlive() ? null
                    : ep.getDateOfDeath()
                            .map(d -> (d.getOperator() == Date.Operator.ABT || d.getOperator() == Date.Operator.EST)
                                    ? "aprox. " + d.getYear().getValue()
                                    : String.valueOf(d.getYear().getValue()))
                            .orElse(null);

            // ── Places ──────────────────────────────────────────────────
            String birthPlace = ep.getPlaceOfBirth().map(Place::name).orElse(null);
            String deathPlace = ep.getPlaceOfDeath().map(Place::name).orElse(null);

            sb.append(String.format(
                    "  {\"title\": %s, \"titleFull\": %s, \"givenName\": %s, \"surname\": %s, \"nickname\": %s, \"birthYear\": %s, \"deathYear\": %s, \"birthPlace\": %s, \"deathPlace\": %s}%s\n",
                    jsonStr(title), jsonStr(titleFull), jsonStr(givenName), jsonStr(surname), jsonStr(nickname),
                    jsonStr(birthYear), jsonStr(deathYear), jsonStr(birthPlace), jsonStr(deathPlace),
                    i < personalities.size() - 1 ? "," : ""));
        }
        sb.append("]");

        Path output = Path.of("../geneaazul-web/data/personalities.json");
        Files.writeString(output, sb);
        System.out.println("Written: " + output.toAbsolutePath());
    }

    private static String jsonStr(@Nullable String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private record TimelineEntry(
            int year,
            Integer month,
            Integer day,
            String type,
            String title,
            String body,
            String source,
            @Nullable String sourceUrl,
            @Nullable String storySlug,
            @Nullable String imageUrl) {}

    /**
     * Parse a timeline Markdown file. Format:
     * <pre>
     * ---
     * year: 1832
     * month: 12       # or null
     * day: 16         # or null
     * type: ...
     * title: ...
     * source: ...
     * sourceUrl: ...  # or null
     * imageUrl: ...   # or null
     * storySlug: ...  # optional
     * ---
     *
     * Body (1-3 sentences).
     * </pre>
     */
    private static TimelineEntry parseTimelineMarkdown(Path file) {
        try {
            String content = Files.readString(file);
            if (!content.startsWith("---")) {
                throw new IllegalStateException("Missing frontmatter in " + file);
            }
            int end = content.indexOf("\n---", 3);
            if (end < 0) {
                throw new IllegalStateException("Unterminated frontmatter in " + file);
            }
            String frontmatter = content.substring(3, end).trim();
            String body = content.substring(end + 4).trim();

            Map<String, String> fields = new LinkedHashMap<>();
            for (String line : frontmatter.split("\n")) {
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                fields.put(key, value);
            }

            return new TimelineEntry(
                    Integer.parseInt(fields.get("year")),
                    parseNullableInt(fields.get("month")),
                    parseNullableInt(fields.get("day")),
                    fields.get("type"),
                    fields.get("title"),
                    body,
                    fields.get("source"),
                    parseNullableString(fields.get("sourceUrl")),
                    parseNullableString(fields.get("storySlug")),
                    parseNullableString(fields.get("imageUrl")));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read " + file, ex);
        }
    }

    @Nullable
    private static Integer parseNullableInt(@Nullable String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) return null;
        return Integer.parseInt(value);
    }

    @Nullable
    private static String parseNullableString(@Nullable String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) return null;
        return value;
    }

    @Test
    public void generateTimelineJson() throws IOException {

        List<TimelineEntry> entries = new ArrayList<>();

        // ── Historia, genealogia and descubrimiento entries (from Markdown resources) ──
        // Each file under src/test/resources/timeline/{history|genealogy|discovery} is one entry.
        // Format: YAML-ish frontmatter (year, month, day, type, title, source, sourceUrl, imageUrl, storySlug)
        // followed by the body as free text.
        Path timelineDir = Path.of("src/test/resources/timeline");
        try (var paths = Files.walk(timelineDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".md"))
                    .forEach(p -> entries.add(parseTimelineMarkdown(p)));
        }

        // ── Genealogia entries (derived from GEDCOM) ──────────────────
        String azulPlace = "Azul, Buenos Aires, Argentina";
        List<EnrichedPerson> azulPeople = searchService
                .findPersonsByPlaceOfAnyEvent(azulPlace, null, null, false, false, false, gedcom.getPeople());

        // Earliest birth year of a person with any event in Azul
        azulPeople.stream()
                .filter(p -> p.getDateOfBirth().isPresent())
                .min(Comparator.comparingInt(p -> p.getDateOfBirth().get().getYear().getValue()))
                .ifPresent(p -> {
                    int year = p.getDateOfBirth().get().getYear().getValue();
                    entries.add(new TimelineEntry(year, null, null, "genealogia",
                            "Nacimiento más antiguo en el árbol de Azul",
                            "El árbol genealógico de Genea Azul registra personas vinculadas a Azul con nacimientos desde " + year + ".",
                            "GEDCOM — Genea Azul", null, null, null));
                });

        // First Italian immigrant (earliest birth year) with any event in Azul
        azulPeople.stream()
                .filter(p -> p.getPlaceOfBirth()
                        .map(place -> "Italia".equals(place.country()))
                        .orElse(false))
                .filter(p -> p.getDateOfBirth().isPresent())
                .min(Comparator.comparingInt(p -> p.getDateOfBirth().get().getYear().getValue()))
                .ifPresent(p -> {
                    int year = p.getDateOfBirth().get().getYear().getValue();
                    entries.add(new TimelineEntry(year, null, null, "genealogia",
                            "Primera inmigración italiana registrada en Azul",
                            "El árbol genealógico registra el primer inmigrante de origen italiano con eventos en Azul nacido hacia " + year + ". Italia es la comunidad inmigrante más representada en el partido.",
                            "GEDCOM — Genea Azul", null, null, "img/timeline/flag-it.svg"));
                });

        // Most common surname in Azul: year of earliest appearance
        List<GedcomAnalyzerService.SurnamesCardinality> surnameCounts = gedcomAnalyzerService
                .getSurnamesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), azulPlace, null, true, false);
        if (!surnameCounts.isEmpty()) {
            GedcomAnalyzerService.SurnamesCardinality top = surnameCounts.getFirst();
            int earliestYear = azulPeople.stream()
                    .filter(p -> p.getSurname()
                            .map(s -> s.normalizedMainWord().equals(top.mainSurname().normalizedMainWord()))
                            .orElse(false))
                    .filter(p -> p.getDateOfBirth().isPresent())
                    .mapToInt(p -> p.getDateOfBirth().get().getYear().getValue())
                    .min()
                    .orElse(1900);
            entries.add(new TimelineEntry(earliestYear, null, null, "genealogia",
                    "El apellido más frecuente en Azul: " + top.mainSurname().value(),
                    top.value() + " personas con el apellido " + top.mainSurname().value()
                            + " tienen eventos registrados en Azul, convirtiéndolo en el más frecuente del árbol genealógico de Genea Azul.",
                    "GEDCOM — Genea Azul", null, null, null));
        }

        // ── Sort: year asc, month asc (nulls last), day asc (nulls last) ──
        entries.sort(Comparator
                .comparingInt(TimelineEntry::year)
                .thenComparingInt(e -> e.month() != null ? e.month() : 99)
                .thenComparingInt(e -> e.day() != null ? e.day() : 99));

        // ── Emit JSON ─────────────────────────────────────────────────
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            TimelineEntry e = entries.get(i);
            sb.append(String.format(
                    "  {\"year\": %d, \"month\": %s, \"day\": %s, \"type\": %s, \"title\": %s, \"body\": %s, \"source\": %s, \"sourceUrl\": %s, \"storySlug\": %s, \"imageUrl\": %s}%s\n",
                    e.year(),
                    e.month() != null ? e.month() : "null",
                    e.day() != null ? e.day() : "null",
                    jsonStr(e.type()),
                    jsonStr(e.title()),
                    jsonStr(e.body()),
                    jsonStr(e.source()),
                    jsonStr(e.sourceUrl()),
                    jsonStr(e.storySlug()),
                    jsonStr(e.imageUrl()),
                    i < entries.size() - 1 ? "," : ""));
        }
        sb.append("]");

        Path output = Path.of("../geneaazul-web/data/timeline.json");
        Files.writeString(output, sb);
        System.out.println("Written: " + output.toAbsolutePath());
    }

}
