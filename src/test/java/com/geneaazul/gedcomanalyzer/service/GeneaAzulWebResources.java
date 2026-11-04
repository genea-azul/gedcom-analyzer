package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.analysis.GedcomStatisticsService;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.SubGedcomOptions;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.AlivePersonFilter;
import com.geneaazul.gedcomanalyzer.service.familytree.FamilyTreeHelper;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.NameUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.visitors.GedcomWriter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.annotation.Nullable;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class GeneaAzulWebResources {

    /* Tree photos are served by the site (Cloudflare), so tree pages don't wake the backend (billed while awake) */
    private static final String PROFILE_PICTURE_URL_PREFIX = "/img/photos/";
    /* The production photo cache, whose keys survive re-exports; the site's photo files are copied from it */
    private static final String PHOTO_SOURCE_URL_PREFIX = "https://gedcom-analyzer-app.fly.dev/api/photos/";
    private static final Pattern SITE_PHOTO = Pattern.compile("^2 FILE /img/photos/([0-9a-f]{24})\\.jpg$", Pattern.MULTILINE);

    public static final int MAX_SURNAMES_PER_COUNTRY = 22;

    @Autowired
    private GedcomStatisticsService gedcomStatisticsService;
    @Autowired
    private GedcomHolder gedcomHolder;
    /*
    @Autowired
    private SearchService searchService;
    */
    @Autowired
    private SurnameService surnameService;
    @Autowired
    private FamilyTreeHelper familyTreeHelper;
    @Autowired
    private GedcomParsingService gedcomParsingService;

    private EnrichedGedcom gedcom;

    @BeforeEach
    public void setUp() {
        gedcom = gedcomHolder.getGedcom();
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
        COUNTRY_ISO.put("Océano Atlántico",     "OCEAN");
        COUNTRY_ISO.put("Australia",            "AU");
        COUNTRY_ISO.put("Bulgaria",             "BG");
        COUNTRY_ISO.put("China",                "CN");
        COUNTRY_ISO.put("Cuba",                 "CU");
        COUNTRY_ISO.put("Irlanda del Norte",    "GB-NIR");
        COUNTRY_ISO.put("Jamaica",              "JM");
        COUNTRY_ISO.put("Nicaragua",            "NI");
        COUNTRY_ISO.put("República Dominicana", "DO");
        COUNTRY_ISO.put("Rumania",              "RO");
        COUNTRY_ISO.put("Moldavia",             "MD");
    }

    /** Root person id → the marriage the web tree opens on ({@code _ROOT_FAM}); unlisted roots show every marriage. */
    private static final Map<Integer, Integer> ROOT_FAMILIES = Map.of(
            // Gennuso: Giuseppe's 1st marriage, to Domenica Vitale (Dima's line)
            557058, 519904);

    record SubGedcomConfig(
            Integer personId,
            boolean directLineageOnly,
            @Nullable Integer trimTriggerSize,
            int maxAscDepth,
            int maxDescDepth,
            @Nullable Integer distantAncestorDescLimit,
            boolean includeInLawsAtMaxDescDepth,
            boolean includeInLawsAtMaxAscDepth,
            @Nullable Integer maxCollateralDescDepth,
            boolean includeSpouseAncestors) {}

    @Test
    public void generateWebSubGedcoms() throws IOException, InterruptedException {
        // Add sub-gedcom configs here (person IDs to be provided):
        List<SubGedcomConfig> configs = List.of(
                // Losardo
                new SubGedcomConfig(512563, true, 0, 1, 4, null, true, true, null, true),
                // Piazza
                new SubGedcomConfig(572, true, 0, 1, 4, null, true, true, null, true),
                // Gennuso
                new SubGedcomConfig(557058, true, 0, 1, 4, null, true, true, null, true),
                // Catriel
                new SubGedcomConfig(511661, true, 0, 1, 4, null, true, true, null, true),
                // Labaronnie
                new SubGedcomConfig(549805, true, 0, 1, 4, null, true, true, null, true),
                // Acosta
                new SubGedcomConfig(530481, true, 0, 1, 4, null, true, true, null, true),
                // Solano
                new SubGedcomConfig(530416, true, 0, 1, 4, null, true, true, null, true),
                // Begbeder
                new SubGedcomConfig(505373, true, 0, 1, 4, null, true, true, null, true),
                // Dhérété
                new SubGedcomConfig(529854, true, 0, 1, 4, null, true, true, null, true),
                // Gallicchio
                new SubGedcomConfig(509443, true, 0, 1, 4, null, true, true, null, true),
                // Falconaro
                new SubGedcomConfig(516988, true, 0, 1, 3, null, true, true, null, true),
                // Mandagaran
                new SubGedcomConfig(511254, true, 0, 1, 4, null, true, true, null, true),
                // Émbil
                new SubGedcomConfig(510902, true, 0, 1, 4, null, true, true, null, true),
                // Moroni
                new SubGedcomConfig(515935, true, 0, 1, 4, null, true, true, null, true),
                // Ciano
                new SubGedcomConfig(504901, true, 0, 1, 4, null, true, true, null, true),
                // Pérez de Villarreal
                new SubGedcomConfig(512049, true, 0, 1, 4, null, true, true, null, true),
                // Picot
                new SubGedcomConfig(504252, true, 0, 1, 4, null, true, true, null, true),
                // Saparrat
                new SubGedcomConfig(503103, true, 0, 1, 4, null, true, true, null, true),
                // Cachenaut
                new SubGedcomConfig(317, true, 0, 1, 4, null, true, true, null, true),
                // Lier
                new SubGedcomConfig(526, true, 0, 1, 4, null, true, true, null, true),
                // Laddaga
                new SubGedcomConfig(543443, true, 0, 1, 4, null, true, true, null, true),
                // Grierson
                new SubGedcomConfig(515268, true, 0, 1, 4, null, true, true, null, true),
                // Mirande
                new SubGedcomConfig(517810, true, 0, 1, 3, null, true, true, null, true),
                // Le Vigne
                new SubGedcomConfig(521667, true, 0, 1, 4, null, true, true, null, true),
                // Tumminaro
                new SubGedcomConfig(503406, true, 0, 1, 4, null, true, true, null, true),
                // Sombra
                new SubGedcomConfig(536853, true, 0, 1, 3, null, true, true, null, true),
                // Adrogué
                new SubGedcomConfig(508992, true, 0, 1, 3, null, true, true, null, true),
                // Castellár
                new SubGedcomConfig(506750, true, 0, 1, 4, null, true, true, null, true),
                // Dhers
                new SubGedcomConfig(515508, true, 0, 1, 4, null, true, true, null, true),
                // Bergoglio
                new SubGedcomConfig(525144, true, 0, 1, 4, null, true, true, null, true),
                // Bourdette
                new SubGedcomConfig(506001, true, 0, 1, 4, null, true, true, null, true),
                // Pomphile
                new SubGedcomConfig(517820, true, 0, 1, 4, null, true, true, null, true),
                // Hournau
                new SubGedcomConfig(503270, true, 0, 1, 4, null, true, true, null, true),
                // Cordeviola
                new SubGedcomConfig(505564, true, 0, 1, 3, null, true, true, null, true),
                // Duclós
                new SubGedcomConfig(570958, true, 0, 1, 3, null, true, true, null, true),
                // Génova
                new SubGedcomConfig(558022, true, 0, 1, 4, null, true, true, null, true),
                // Navas
                new SubGedcomConfig(558017, true, 0, 1, 4, null, true, true, null, true),
                // Maicá
                new SubGedcomConfig(517457, true, 0, 1, 4, null, true, true, null, true),
                // Mastantuono
                new SubGedcomConfig(528443, true, 0, 1, 3, null, true, true, null, true),
                // Mailharro
                new SubGedcomConfig(502886, true, 0, 1, 4, null, true, true, null, true),
                // Arrouy
                new SubGedcomConfig(504124, true, 0, 1, 4, null, true, true, null, true),
                // Prat
                new SubGedcomConfig(503006, true, 0, 1, 4, null, true, true, null, true),
                // Zabala
                new SubGedcomConfig(503479, true, 0, 1, 4, null, true, true, null, true),
                // Arguiano
                new SubGedcomConfig(539276, true, 0, 1, 4, null, true, true, null, true),
                // Puppio
                new SubGedcomConfig(504654, true, 0, 1, 2, null, true, true, null, true),
                // Álvaro
                new SubGedcomConfig(504669, true, 0, 1, 3, null, true, true, null, true),
                // Picaroni
                new SubGedcomConfig(511915, true, 0, 1, 3, null, true, true, null, true),
                // Grippaldi
                new SubGedcomConfig(505769, true, 0, 1, 3, null, true, true, null, true),
                // Ciminelli
                new SubGedcomConfig(178, true, 0, 1, 3, null, true, true, null, true),
                // Sparaíno
                new SubGedcomConfig(512657, true, 0, 1, 3, null, true, true, null, true),
                // Basile
                new SubGedcomConfig(512663, true, 0, 1, 3, null, true, true, null, true),
                // Arrubia
                new SubGedcomConfig(523471, true, 0, 1, 3, null, true, true, null, true),
                // Arrastúa
                new SubGedcomConfig(483, true, 0, 1, 3, null, true, true, null, true),
                // Arrastía
                new SubGedcomConfig(502162, true, 0, 1, 3, null, true, true, null, true),
                // Petersen
                new SubGedcomConfig(543211, true, 0, 1, 4, null, true, true, null, true),
                // Yannuzzi
                new SubGedcomConfig(508901, true, 0, 1, 4, null, true, true, null, true),
                // Cornec
                new SubGedcomConfig(552123, true, 0, 1, 3, null, true, true, null, true),
                // Layús
                new SubGedcomConfig(531723, true, 0, 1, 4, null, true, true, null, true),
                // Larrocca
                new SubGedcomConfig(503673, true, 0, 1, 3, null, true, true, null, true),
                // Olza
                new SubGedcomConfig(556324, true, 0, 1, 3, null, true, true, null, true),
                // Borneo
                new SubGedcomConfig(511072, true, 0, 1, 4, null, true, true, null, true),
                // Marateo
                new SubGedcomConfig(529942, true, 0, 1, 4, null, true, true, null, true),
                // Guedes
                new SubGedcomConfig(540210, true, 0, 1, 4, null, true, true, null, true),
                // carus -> Carús
                new SubGedcomConfig(511262, true, 0, 1, 2, null, true, true, null, true),
                // marquestau -> Marquestau
                new SubGedcomConfig(546785, true, 0, 1, 2, null, true, true, null, true),
                // kollmann -> Kollmann
                new SubGedcomConfig(513459, true, 0, 1, 4, null, true, true, null, true),
                // bardelli -> Bardelli
                new SubGedcomConfig(569559, true, 0, 1, 2, null, true, true, null, true),
                // comparato -> Comparato
                new SubGedcomConfig(520424, true, 0, 1, 3, null, true, true, null, true),
                // oyarzabal -> Oyarzábal
                new SubGedcomConfig(525017, true, 0, 1, 3, null, true, true, null, true),
                // cier -> Cier
                new SubGedcomConfig(515869, true, 0, 1, 3, null, true, true, null, true),
                // claverie -> Claveríe
                new SubGedcomConfig(505897, true, 0, 1, 2, null, true, true, null, true),
                // brescia -> Brescia
                new SubGedcomConfig(505878, true, 0, 1, 3, null, true, true, null, true),
                // camarotte -> Camarotte
                new SubGedcomConfig(505872, true, 0, 1, 3, null, true, true, null, true),
                // toscano -> Toscano
                new SubGedcomConfig(507508, true, 0, 1, 4, null, true, true, null, true),
                // azimonti -> Azimonti
                new SubGedcomConfig(540543, true, 0, 1, 2, null, true, true, null, true),
                // forestieri -> Forastieri
                new SubGedcomConfig(527528, true, 0, 1, 3, null, true, true, null, true),
                // testavin-touron -> Turón
                new SubGedcomConfig(503774, true, 0, 1, 4, null, true, true, null, true),
                // di-lergna -> Dilernia
                new SubGedcomConfig(514689, true, 0, 1, 4, null, true, true, null, true),
                // giangrande -> Giangrande
                new SubGedcomConfig(521437, true, 0, 1, 4, null, true, true, null, true),
                // despervasques -> Desperbasques
                new SubGedcomConfig(547605, true, 0, 1, 4, null, true, true, null, true),
                // dours -> Dours
                new SubGedcomConfig(505242, true, 0, 1, 4, null, true, true, null, true),
                // pontot -> Ponthot
                new SubGedcomConfig(505245, true, 0, 1, 4, null, true, true, null, true),
                // tellechea -> Tellechea
                new SubGedcomConfig(535058, true, 0, 1, 4, null, true, true, null, true),
                // garciarena-zuloaga -> Garciarena (Berástegui, España)
                new SubGedcomConfig(505047, true, 0, 1, 4, null, true, true, null, true),
                // latronico -> Latrónica
                new SubGedcomConfig(512306, true, 0, 1, 4, null, true, true, null, true),
                // garciarena-y-mariezcurrena -> Garciarena (Ezcurra, España)
                new SubGedcomConfig(511651, true, 0, 1, 4, null, true, true, null, true),
                // montenegro -> Montenegro
                new SubGedcomConfig(503342, true, 0, 1, 4, null, true, true, null, true),
                // baldovino -> Baldovino
                new SubGedcomConfig(503474, true, 0, 1, 4, null, true, true, null, true),
                // sahaspe -> Sahaspé
                new SubGedcomConfig(546736, true, 0, 1, 4, null, true, true, null, true),
                // ghisoli -> Ghissoli
                new SubGedcomConfig(512868, true, 0, 1, 3, null, true, true, null, true),
                // iztueta -> Iztueta
                new SubGedcomConfig(521747, true, 0, 1, 4, null, true, true, null, true),
                // pinero-1 -> Piñero (Dolores, Argentina)
                new SubGedcomConfig(524083, true, 0, 1, 3, null, true, true, null, true),
                // pinero-2 -> Piñero (Dolores, Argentina)
                new SubGedcomConfig(525514, true, 0, 1, 3, null, true, true, null, true),
                // bohn -> Bohn
                new SubGedcomConfig(551685, true, 0, 1, 4, null, true, true, null, true),
                // vignau-1 -> Vignau (Maslacq)
                new SubGedcomConfig(526595, true, 0, 1, 4, null, true, true, null, true),
                // vignau-2 -> Vignau (Oloron-Sainte-Marie)
                new SubGedcomConfig(519553, true, 0, 1, 3, null, true, true, null, true),
                // loustau -> Loustau
                new SubGedcomConfig(509020, true, 0, 1, 3, null, true, true, null, true),
                // hollmann -> Holman
                new SubGedcomConfig(568869, true, 0, 1, 3, null, true, true, null, true),
                // salamone -> Salamone
                new SubGedcomConfig(573202, true, 0, 1, 3, null, true, true, null, true),
                // santopaolo -> Santopaolo
                new SubGedcomConfig(546202, true, 0, 1, 3, null, true, true, null, true),
                // lopez-claro -> López Claro
                new SubGedcomConfig(504548, true, 0, 1, 3, null, true, true, null, true),
                // stickar -> Stickar
                new SubGedcomConfig(557255, true, 0, 1, 3, null, true, true, null, true),
                // severiens -> Severiens
                new SubGedcomConfig(527946, true, 0, 1, 4, null, true, true, null, true),
                // calderaro -> Calderaro
                new SubGedcomConfig(509932, true, 0, 1, 3, null, true, true, null, true),
                // duhalde -> Duhalde
                new SubGedcomConfig(541678, true, 0, 1, 4, null, true, true, null, true),
                // chrestia -> Chrestía
                new SubGedcomConfig(565508, true, 0, 1, 4, null, true, true, null, true),
                // dalessandro-1 -> D'Alessandro (Rotondella, Italia)
                new SubGedcomConfig(543161, true, 0, 1, 4, null, true, true, null, true),
                // dalessandro-2 -> D'Alessandro (Rotondella, Italia)
                new SubGedcomConfig(554795, true, 0, 1, 4, null, true, true, null, true),
                // dalessandro-3 -> D'Alessandro (Rotonda, Italia)
                new SubGedcomConfig(532881, true, 0, 1, 4, null, true, true, null, true),
                // dalessandro-4 -> D'Alessandro (Italia)
                new SubGedcomConfig(547649, true, 0, 1, 4, null, true, true, null, true),
                // sarmiento -> Sarmiento
                new SubGedcomConfig(546231, true, 0, 1, 4, null, true, true, null, true),
                // avila -> Ávila
                new SubGedcomConfig(543577, true, 0, 1, 3, null, true, true, null, true),
                // de-bonis -> Delbonis
                new SubGedcomConfig(504002, true, 0, 1, 3, null, true, true, null, true),
                // scalcini -> Scalcini
                new SubGedcomConfig(512031, true, 0, 1, 4, null, true, true, null, true),
                // zazzali -> Sassali
                new SubGedcomConfig(550610, true, 0, 1, 4, null, true, true, null, true),
                // louge-1 -> Louge (Sédeilhac, Francia)
                new SubGedcomConfig(523372, true, 0, 1, 4, null, true, true, null, true),
                // louge-2 -> Louge (Nizan-Gesse, Francia)
                new SubGedcomConfig(512615, true, 0, 1, 4, null, true, true, null, true),
                // armentano-1 -> Armentano (San Severino Lucano, Italia)
                new SubGedcomConfig(500699, true, 0, 1, 4, null, true, true, null, true),
                // armentano-2 -> Armentano (San Severino Lucano, Italia)
                new SubGedcomConfig(16, true, 0, 1, 4, null, true, true, null, true),
                // sarno -> Sarno
                new SubGedcomConfig(517062, true, 0, 1, 4, null, true, true, null, true),
                // prezioso -> Prezioso
                new SubGedcomConfig(509495, true, 0, 1, 4, null, true, true, null, true),
                // cuccaro -> Cúccaro
                new SubGedcomConfig(515301, true, 0, 1, 4, null, true, true, null, true),
                // cupparo -> Cúpparo
                new SubGedcomConfig(508353, true, 0, 1, 4, null, true, true, null, true),
                // torchia-1 -> Torchia (Pittarella, Italia)
                new SubGedcomConfig(521227, true, 0, 1, 4, null, true, true, null, true),
                // torchia-2 -> Torchia (Pittarella, Italia)
                new SubGedcomConfig(529306, true, 0, 1, 4, null, true, true, null, true),
                // riviere -> Rivière
                new SubGedcomConfig(562114, true, 0, 1, 5, null, true, true, null, true),
                // lurbe -> Lurbé
                new SubGedcomConfig(532738, true, 0, 1, 5, null, true, true, null, true),
                // collazo -> Collazo
                new SubGedcomConfig(515982, true, 0, 1, 4, null, true, true, null, true),
                // marmouget -> Marmouget
                new SubGedcomConfig(561682, true, 0, 1, 5, null, true, true, null, true),
                // cirigliano -> Cirigliano
                new SubGedcomConfig(8, true, 0, 1, 3, null, true, true, null, true),
                // maddio -> Maddío
                new SubGedcomConfig(508394, true, 0, 1, 5, null, true, true, null, true),
                // galizio -> Galizio
                new SubGedcomConfig(512272, true, 0, 1, 5, null, true, true, null, true),
                // draghi -> Draghi
                new SubGedcomConfig(553990, true, 0, 1, 6, null, true, true, null, true),
                // del-buono -> Del Buono
                new SubGedcomConfig(504488, true, 0, 1, 6, null, true, true, null, true),
                // dascenzo -> Dacenzo
                new SubGedcomConfig(523136, true, 0, 1, 5, null, true, true, null, true),
                // rui -> Rui
                new SubGedcomConfig(557873, true, 0, 1, 3, null, true, true, null, true),
                // lamanna -> Lamanna
                new SubGedcomConfig(500611, true, 0, 1, 5, null, true, true, null, true),
                // ballarena -> Ballarena
                new SubGedcomConfig(532065, true, 0, 1, 5, null, true, true, null, true),
                // lapenta -> Lapenta
                new SubGedcomConfig(514813, true, 0, 1, 4, null, true, true, null, true),
                // ahets-etcheberry -> Ahets Etcheberry
                new SubGedcomConfig(531482, true, 0, 1, 5, null, true, true, null, true),
                // daulerio -> Daulerio
                new SubGedcomConfig(505600, true, 0, 1, 4, null, true, true, null, true),
                // berdinas -> Berdiñas
                new SubGedcomConfig(506471, true, 0, 1, 6, null, true, true, null, true),
                // anon -> Añón
                new SubGedcomConfig(517723, true, 0, 1, 6, null, true, true, null, true),
                // canalicchio -> Canalicchio
                new SubGedcomConfig(510687, true, 0, 1, 4, null, true, true, null, true),
                // miracco -> Miracco
                new SubGedcomConfig(522585, true, 0, 1, 6, null, true, true, null, true),
                // etchebertz -> Etchevers
                new SubGedcomConfig(522523, true, 0, 1, 4, null, true, true, null, true),
                // mongay -> Mongay
                new SubGedcomConfig(529022, true, 0, 1, 5, null, true, true, null, true)
        );

        Path outputDir = Path.of("../geneaazul-web/data/gedcom");
        Files.createDirectories(outputDir);

        // Pre-calculate the filename suffix for every config so that surnames that map to the same
        // simplified string (e.g. two distinct Garciarena or Vignau families) receive a -1/-2/-3
        // disambiguation suffix rather than silently overwriting each other's output file.
        List<String> rawLastnames = configs.stream()
                .map(config -> Objects.requireNonNull(gedcom.getPersonById(config.personId()),
                        "Person not found: I" + config.personId())
                        .getSurname()
                        .map(Surname::simplified)
                        .map(surname -> StringUtils.replaceChars(surname, ' ', '-'))
                        .orElseGet(() -> config.personId().toString()))
                .toList();
        List<String> personLastnames = assignLastnameSuffixes(rawLastnames);

        for (int i = 0; i < configs.size(); i++) {
            SubGedcomConfig config = configs.get(i);
            EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(config.personId()),
                    "Person not found: I" + config.personId());

            List<List<Relationship>> relationshipsList = familyTreeHelper.getRelationshipsWithNotInLawPriority(
                person,
                 config.includeSpouseAncestors());

            System.out.printf("generateWebSubGedcoms: I%d %s — people in tree: %d%n",
                    config.personId(), person.getDisplayName(), relationshipsList.size());

            org.folg.gedcom.model.Gedcom subGedcom = formatSubGedcom(config, relationshipsList);

            Path output = outputDir.resolve("sub-gedcom-" + personLastnames.get(i) + ".ged");
            boolean changed = gedcomParsingService.writeIfChanged(subGedcom, output);
            System.out.println((changed ? "Written: " : "Unchanged, skipped: ") + output.toAbsolutePath());
        }

        copyMissingPhotos(outputDir, Path.of("../geneaazul-web/img/photos"));
    }

    private org.folg.gedcom.model.Gedcom formatSubGedcom(SubGedcomConfig config, List<List<Relationship>> relationshipsList) {
        return gedcomParsingService.format(
                gedcom.getLegacyGedcom().get(),
                relationshipsList,
                SubGedcomOptions.builder()
                        .alivePersonFilter(AlivePersonFilter.SHOW_SURNAME_ONLY)
                        .displayOnlyBasic(true)
                        // Photos served from the production cache (keys are stable across exports)
                        .profilePictureUrlPrefix(PROFILE_PICTURE_URL_PREFIX)
                        .directLineageOnly(config.directLineageOnly())
                        .rootPersonId(config.personId())
                        .rootFamilyId(ROOT_FAMILIES.get(config.personId()))
                        .trimTriggerSize(config.trimTriggerSize())
                        .maxAscDepth(config.maxAscDepth())
                        .maxDescDepth(config.maxDescDepth())
                        .distantAncestorDescLimit(config.distantAncestorDescLimit())
                        .includeInLawsAtMaxDescDepth(config.includeInLawsAtMaxDescDepth())
                        .includeInLawsAtMaxAscDepth(config.includeInLawsAtMaxAscDepth())
                        .maxCollateralDescDepth(config.maxCollateralDescDepth())
                        .build());
    }

    /**
     * Helps add a web tree (geneaazul-web's add-family-tree skill). With {@code -Dtree-query="given surname"}
     * it lists the matching people; with {@code -Dtree-person-id=N} (or {@code N,M,…}), each person's marriages, descendant
     * generations (alive people are shown as {@code <privado>} on the site) and the tree size for each
     * {@code maxDescDepth}. Skipped when neither is given.
     */
    @Test
    public void inspectWebTreeCandidate() throws IOException {
        String query = System.getProperty("tree-query");
        String personId = System.getProperty("tree-person-id");
        Assumptions.assumeTrue(query != null || personId != null, "Pass -Dtree-query or -Dtree-person-id");
        if (query != null) {
            printTreeCandidates(query);
        }
        if (personId != null) {
            for (String id : personId.split(",")) {
                printTreeOptions(Integer.valueOf(id.strip()));
            }
        }
    }

    private void printTreeCandidates(String query) {
        List<EnrichedPerson> matches = gedcom.getPeople().stream()
                .filter(p -> matchesNameQuery(p.getDisplayName(), query))
                .toList();
        System.out.printf("TREE-CANDIDATES for \"%s\": %d%n", query, matches.size());
        matches.stream().limit(40).forEach(p -> System.out.printf(
                "TREE-CANDIDATE I%d | %s | %s | b. %s %s | d. %s %s | parents: %s | spouses: %s | children: %d%n",
                p.getId(), p.getDisplayName(), p.isAlive() ? "alive" : "deceased",
                p.getDateOfBirth().map(Date::getYear).map(String::valueOf).orElse("?"),
                p.getPlaceOfBirth().map(Place::name).orElse(""),
                p.getDateOfDeath().map(Date::getYear).map(String::valueOf).orElse("?"),
                p.getPlaceOfDeath().map(Place::name).orElse(""),
                p.getParents().stream().map(EnrichedPerson::getDisplayName).collect(Collectors.joining(" & ")),
                p.getSpouses().stream().map(EnrichedPerson::getDisplayName).collect(Collectors.joining(", ")),
                p.getChildren().size()));
    }

    private void printTreeOptions(Integer personId) throws IOException {
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(personId), "Person not found: I" + personId);
        String slug = person.getSurname()
                .map(Surname::simplified)
                .map(surname -> StringUtils.replaceChars(surname, ' ', '-'))
                .orElseGet(personId::toString);
        System.out.printf("TREE-PERSON I%d %s | slug: %s%n", personId, person.getDisplayName(), slug);

        org.folg.gedcom.model.Gedcom legacy = gedcom.getLegacyGedcom().orElseThrow();
        for (org.folg.gedcom.model.SpouseFamilyRef ref : person.getLegacyPerson().orElseThrow().getSpouseFamilyRefs()) {
            org.folg.gedcom.model.Family family = legacy.getFamily(ref.getRef());
            String spouse = Stream.concat(family.getHusbandRefs().stream(), family.getWifeRefs().stream())
                    .map(org.folg.gedcom.model.SpouseRef::getRef)
                    .filter(spouseRef -> !spouseRef.equals("I" + personId))
                    .map(spouseRef -> gedcom.getPersonById(Integer.valueOf(spouseRef.substring(1))))
                    .filter(Objects::nonNull)
                    .map(EnrichedPerson::getDisplayName)
                    .findFirst()
                    .orElse("(no spouse)");
            String date = family.getEventsFacts().stream()
                    .filter(e -> "MARR".equals(e.getTag()) && e.getDate() != null)
                    .map(org.folg.gedcom.model.EventFact::getDate)
                    .findFirst()
                    .orElse("?");
            System.out.printf("TREE-MARRIAGE %s | %s | married %s | children: %d%n",
                    family.getId(), spouse, date, family.getChildRefs().size());
        }

        List<List<EnrichedPerson>> generations = descendantGenerations(person);
        for (int g = 0; g < generations.size(); g++) {
            List<EnrichedPerson> generation = generations.get(g);
            long alive = generation.stream().filter(EnrichedPerson::isAlive).count();
            System.out.printf("TREE-GENERATION %d | people: %d | alive: %d%s%n",
                    g + 1, generation.size(), alive, alive == generation.size() ? " (all alive)" : "");
        }

        List<List<Relationship>> relationshipsList = familyTreeHelper.getRelationshipsWithNotInLawPriority(person, true);
        for (int depth = 1; depth <= generations.size() + 1; depth++) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new GedcomWriter().write(formatSubGedcom(webTreeConfig(personId, depth), relationshipsList), out);
            String text = out.toString(StandardCharsets.UTF_8);
            System.out.printf("TREE-DEPTH %d | people: %d | private: %d%n", depth,
                    Pattern.compile("^0 @I\\d+@ INDI$", Pattern.MULTILINE).matcher(text).results().count(),
                    Pattern.compile("^1 NAME <privado>", Pattern.MULTILINE).matcher(text).results().count());
        }
        System.out.printf("TREE-SUGGESTED-DEPTH %d%n", suggestedMaxDescDepth(generations.stream()
                .map(generation -> generation.stream().allMatch(EnrichedPerson::isAlive))
                .toList()));
    }

    /** The settings every web tree uses; only {@code maxDescDepth} varies between trees */
    static SubGedcomConfig webTreeConfig(Integer personId, int maxDescDepth) {
        return new SubGedcomConfig(personId, true, 0, 1, maxDescDepth, null, true, true, null, true);
    }

    /** Descendants by generation (children first), each person once, at the generation they first appear */
    static List<List<EnrichedPerson>> descendantGenerations(EnrichedPerson root) {
        List<List<EnrichedPerson>> generations = new ArrayList<>();
        Set<Integer> seen = new java.util.HashSet<>(Set.of(root.getId()));
        List<EnrichedPerson> current = List.of(root);
        while (true) {
            List<EnrichedPerson> next = current.stream()
                    .flatMap(p -> p.getChildren().stream())
                    .filter(child -> seen.add(child.getId()))
                    .toList();
            if (next.isEmpty()) {
                return generations;
            }
            generations.add(next);
            current = next;
        }
    }

    /**
     * Deepest {@code maxDescDepth} that adds at most one generation made only of living people: the site
     * shows them as {@code <privado>}, so going further only adds private cards. At least 1.
     */
    static int suggestedMaxDescDepth(List<Boolean> generationAllAlive) {
        int firstAllAlive = generationAllAlive.indexOf(true);
        return Math.max(1, firstAllAlive >= 0 ? firstAllAlive + 1 : generationAllAlive.size());
    }

    @Test
    public void suggestedMaxDescDepth_stopsAtTheFirstGenerationOfOnlyLivingPeople() {
        Assertions.assertThat(suggestedMaxDescDepth(List.of(false, false, false, true, true))).isEqualTo(4);
        Assertions.assertThat(suggestedMaxDescDepth(List.of(false, true, false))).isEqualTo(2);
        Assertions.assertThat(suggestedMaxDescDepth(List.of(false, false))).isEqualTo(2);
        Assertions.assertThat(suggestedMaxDescDepth(List.of(true))).isEqualTo(1);
        Assertions.assertThat(suggestedMaxDescDepth(List.of())).isEqualTo(1);
    }

    /** Every word of the query starts a word of the name, ignoring case and accents */
    static boolean matchesNameQuery(@Nullable String name, String query) {
        if (name == null) {
            return false;
        }
        List<String> words = List.of(foldForSearch(name).split("[^a-z0-9]+"));
        return Stream.of(foldForSearch(query).split("[^a-z0-9]+"))
                .filter(token -> !token.isEmpty())
                .allMatch(token -> words.stream().anyMatch(word -> word.startsWith(token)));
    }

    private static String foldForSearch(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT);
    }

    @Test
    public void matchesNameQuery_ignoresCaseAccentsAndWordOrder() {
        Assertions.assertThat(matchesNameQuery("Nicolás Zacharie Marmouget", "nicolas marmouget")).isTrue();
        Assertions.assertThat(matchesNameQuery("Nicolás Zacharie Marmouget", "Marmouget Nico")).isTrue();
        Assertions.assertThat(matchesNameQuery("José López", "jose lopez")).isTrue();
        Assertions.assertThat(matchesNameQuery("Nicolás Marmouget", "nicolas gennuso")).isFalse();
        Assertions.assertThat(matchesNameQuery("Ana Marmouget", "mouget")).isFalse();
        Assertions.assertThat(matchesNameQuery(null, "ana")).isFalse();
    }

    /** Keys of the site photos ({@code 2 FILE /img/photos/<key>.jpg}) a sub-GEDCOM references */
    static Set<String> photoKeys(String gedcom) {
        Set<String> keys = new TreeSet<>();
        Matcher m = SITE_PHOTO.matcher(gedcom);
        while (m.find()) {
            keys.add(m.group(1));
        }
        return keys;
    }

    /**
     * Copies into the site every photo the sub-GEDCOMs reference that it doesn't have yet, from the
     * production photo cache. Photos missing there are reported; the tree shows a silhouette instead.
     */
    private static void copyMissingPhotos(Path gedcomDir, Path photosDir) throws IOException, InterruptedException {
        Files.createDirectories(photosDir);
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.list(gedcomDir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".ged")).toList()) {
                keys.addAll(photoKeys(Files.readString(file)));
            }
        }
        int copied = 0;
        try (HttpClient http = HttpClient.newHttpClient()) {
            for (String key : keys) {
                Path target = photosDir.resolve(key + ".jpg");
                if (Files.exists(target)) {
                    continue;
                }
                HttpResponse<byte[]> response = http.send(
                        HttpRequest.newBuilder(URI.create(PHOTO_SOURCE_URL_PREFIX + key + ".jpg")).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    Files.write(target, response.body());
                    copied++;
                } else {
                    System.out.println("Photo not available (HTTP " + response.statusCode() + "): " + key);
                }
            }
        }
        System.out.printf("Tree photos: %d referenced, %d copied into %s%n", keys.size(), copied, photosDir.toAbsolutePath());
    }

    /**
     * Given a list of raw lastnames in config order, returns a same-size list where any lastname
     * that appears more than once receives a {@code -1}, {@code -2}, … suffix in order of first
     * appearance. Unique lastnames are returned unchanged.
     * <p>
     * Example: {@code ["garcia", "vignau", "garcia"]} → {@code ["garcia-1", "vignau", "garcia-2"]}
     */
    static List<String> assignLastnameSuffixes(List<String> rawLastnames) {
        Map<String, Long> counts = rawLastnames.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        Map<String, Integer> counters = new LinkedHashMap<>();
        List<String> result = new ArrayList<>(rawLastnames.size());
        for (String raw : rawLastnames) {
            result.add(counts.get(raw) > 1
                    ? raw + "-" + counters.merge(raw, 1, Integer::sum)
                    : raw);
        }
        return result;
    }

    @Test
    public void assignLastnameSuffixes_assignsSuffixesForDuplicates() {
        Assertions.assertThat(assignLastnameSuffixes(List.of()))
                .isEmpty();
        Assertions.assertThat(assignLastnameSuffixes(List.of("garcia")))
                .containsExactly("garcia");
        Assertions.assertThat(assignLastnameSuffixes(List.of("garcia", "perez")))
                .containsExactly("garcia", "perez");
        Assertions.assertThat(assignLastnameSuffixes(List.of("garcia", "garcia")))
                .containsExactly("garcia-1", "garcia-2");
        Assertions.assertThat(assignLastnameSuffixes(List.of("garcia", "perez", "garcia")))
                .containsExactly("garcia-1", "perez", "garcia-2");
        Assertions.assertThat(assignLastnameSuffixes(List.of("a", "a", "b", "a")))
                .containsExactly("a-1", "a-2", "b", "a-3");
    }

    @Test
    public void photoKeys_findsTheSitePhotosOfASubGedcom() {
        String gedcom = """
                0 @I1@ INDI
                1 OBJE
                2 FILE /img/photos/0123456789abcdef01234567.jpg
                0 @I2@ INDI
                1 OBJE
                2 FILE /img/photos/fedcba9876543210fedcba98.jpg
                0 @I3@ INDI
                1 OBJE
                2 FILE https://example.org/acta.jpg
                1 NOTE /img/photos/aaaaaaaaaaaaaaaaaaaaaaaa.jpg
                """;
        Assertions.assertThat(photoKeys(gedcom))
                .containsExactlyInAnyOrder("0123456789abcdef01234567", "fedcba9876543210fedcba98");
    }

    @Test
    public void generateSurnamesJson() throws IOException {
        List<GedcomStatisticsService.SurnamesCardinality> cardinalities = gedcomStatisticsService
                .getSurnamesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", null, true, false);

        // Build a lookup map: normalizedMainWord → alive count
        Map<String, Integer> aliveCountByNormalized = gedcomStatisticsService
                .getSurnamesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", true, true, false)
                .stream()
                .collect(Collectors.toMap(
                        c -> c.mainSurname().normalizedMainWord(),
                        GedcomStatisticsService.SurnamesCardinality::value));

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < cardinalities.size(); i++) {
            GedcomStatisticsService.SurnamesCardinality cardinality = cardinalities.get(i);

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

        List<GedcomStatisticsService.SurnamesByCityCardinality> places = gedcomStatisticsService
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
                        GedcomStatisticsService.PlacePart.COUNTRY);

        // ── Country groupings ──────────────────────────────────────────
        // Maps each raw GEDCOM country name to its canonical display name.
        // Countries not listed here are used as-is.
        record GroupDef(String displayName, @Nullable String formerly, @Nullable String isoCode) {}

        Map<String, GroupDef> groups = new LinkedHashMap<>();
        GroupDef alemaniaRusia  = new GroupDef("Alemania / Rusia",             null,             "DE");
        GroupDef inglaterra     = new GroupDef("Inglaterra",                   null,             "GB-ENG");
        GroupDef siriaLibano    = new GroupDef("Siria / Líbano",               null,             "SY");
        GroupDef yugoslavia     = new GroupDef("Croacia / Eslovenia / Serbia", "Yugoslavia",     "YU");
        GroupDef checoslovaquia = new GroupDef("República Checa / Eslovaquia", "Checoslovaquia", "CS");

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

        for (GedcomStatisticsService.SurnamesByCityCardinality place : places) {
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
                    "  { \"country\": \"%s\", \"formerly\": %s, \"isoCode\": %s, \"count\": %d, \"percentage\": %.2f, \"topSurnames\": %s }%s\n",
                    key.replace("\"", "\\\""), formerlyJson, isoJson, count, pct, surnamesJson,
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

        // ── Load existing labels ─────────────────────────────────────────────
        // Primary key: personId (stable once assigned); fallback: composite key for
        // first run when the existing JSON predates the personId field.
        Path output = Path.of("../geneaazul-web/data/personalities.json");
        Map<Integer, List<String>> labelsByPersonId = new LinkedHashMap<>();
        Map<String, List<String>> labelsByCompositeKey = new LinkedHashMap<>();
        if (Files.exists(output)) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(output.toFile());
            for (JsonNode node : root) {
                JsonNode labelsNode = node.get("labels");
                if (labelsNode == null || !labelsNode.isArray()) continue;
                List<String> labels = new ArrayList<>();
                for (JsonNode label : labelsNode) labels.add(label.asString());
                JsonNode pidNode = node.get("personId");
                if (pidNode != null && !pidNode.isNull()) {
                    labelsByPersonId.put(pidNode.asInt(), labels);
                }
                labelsByCompositeKey.put(personalityCompositeKey(node), labels);
            }
        }

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < personalities.size(); i++) {
            EnrichedPerson ep = personalities.get(i);

            // ── Name parts from legacy Person ──────────────────────────
            String title = null, titleFull = null, givenName = null, surname = null, nickname = null, nameSuffix = null;
            Name name = ep.getLegacyPerson()
                    .filter(lp -> !lp.getNames().isEmpty())
                    .map(lp -> lp.getNames().getFirst())
                    .orElse(null);
            if (name != null) {
                title = StringUtils.trimToNull(name.getPrefix());
                if (title != null) {
                    titleFull = namePrefixesMap.getOrDefault(NameUtils.simplifyName(title), title);
                }
                givenName  = StringUtils.trimToNull(name.getGiven());
                nickname   = StringUtils.trimToNull(name.getNickname());
                String sp  = StringUtils.trimToNull(name.getSurnamePrefix());
                String s   = StringUtils.trimToNull(name.getSurname());
                surname    = sp != null && s != null ? sp + " " + s : (s != null ? s : sp);
                nameSuffix = StringUtils.trimToNull(name.getSuffix());
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

            // ── Labels ──────────────────────────────────────────────────
            List<String> labels = labelsByPersonId.containsKey(ep.getId())
                    ? labelsByPersonId.get(ep.getId())
                    : labelsByCompositeKey.getOrDefault(
                            personalityCompositeKey(givenName, surname, nameSuffix, birthYear, deathYear),
                            List.of());
            if (labels.isEmpty()) {
                System.out.printf("WARNING: No labels for personality I%d — %s%n", ep.getId(), ep.getDisplayName());
            }
            String labelsJson = labels.stream()
                    .map(l -> "\"" + l.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                    .collect(Collectors.joining(", "));

            sb.append(String.format(
                    "  {\"personId\": %d, \"title\": %s, \"titleFull\": %s, \"givenName\": %s, \"surname\": %s, \"nickname\": %s, \"nameSuffix\": %s, \"isAlive\": %s, \"birthYear\": %s, \"deathYear\": %s, \"birthPlace\": %s, \"deathPlace\": %s, \"labels\": [%s]}%s\n",
                    ep.getId(),
                    jsonStr(title), jsonStr(titleFull), jsonStr(givenName), jsonStr(surname), jsonStr(nickname), jsonStr(nameSuffix),
                    ep.isAlive(),
                    jsonStr(birthYear), jsonStr(deathYear), jsonStr(birthPlace), jsonStr(deathPlace),
                    labelsJson,
                    i < personalities.size() - 1 ? "," : ""));
        }
        sb.append("]");

        Files.writeString(output, sb);
        System.out.println("Written: " + output.toAbsolutePath());
    }

    private static String jsonStr(@Nullable String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String personalityCompositeKey(JsonNode node) {
        return personalityCompositeKey(
                node.has("givenName") && !node.get("givenName").isNull() ? node.get("givenName").asString() : null,
                node.has("surname") && !node.get("surname").isNull() ? node.get("surname").asString() : null,
                node.has("nameSuffix") && !node.get("nameSuffix").isNull() ? node.get("nameSuffix").asString() : null,
                node.has("birthYear") && !node.get("birthYear").isNull() ? node.get("birthYear").asString() : null,
                node.has("deathYear") && !node.get("deathYear").isNull() ? node.get("deathYear").asString() : null);
    }

    private static String personalityCompositeKey(
            @Nullable String givenName, @Nullable String surname, @Nullable String nameSuffix,
            @Nullable String birthYear, @Nullable String deathYear) {
        return (givenName != null ? givenName : "") + "|"
                + (surname != null ? surname : "") + "|"
                + (nameSuffix != null ? nameSuffix : "") + "|"
                + (birthYear != null ? birthYear : "") + "|"
                + (deathYear != null ? deathYear : "");
    }

    private record TimelineEntry(
            Integer year,
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
                    parseNullableInt(fields.get("year")),
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

        // ── Historia, genealogia and curiosidades entries (from Markdown resources) ──
        // Each file under src/test/resources/timeline/{history|genealogy|curiosities} is one entry.
        // Format: YAML-ish frontmatter (year, month, day, type, title, source, sourceUrl, imageUrl, storySlug)
        // followed by the body as free text.
        Path timelineDir = Path.of("src/test/resources/timeline");
        try (var paths = Files.walk(timelineDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".md"))
                    .forEach(p -> entries.add(parseTimelineMarkdown(p)));
        }

        /*

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
        List<GedcomStatisticsService.SurnamesCardinality> surnameCounts = gedcomStatisticsService
                .getSurnamesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), azulPlace, null, true, false);
        if (!surnameCounts.isEmpty()) {
            GedcomStatisticsService.SurnamesCardinality top = surnameCounts.getFirst();
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

        */

        // ── Sort: year asc (nulls last), month asc (nulls first), day asc (nulls first) ──
        entries.sort(Comparator
                .comparing(TimelineEntry::year, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TimelineEntry::month, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(TimelineEntry::day, Comparator.nullsFirst(Comparator.naturalOrder())));

        // ── Emit JSON ─────────────────────────────────────────────────
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            TimelineEntry e = entries.get(i);
            sb.append(String.format(
                    "  {\"year\": %s, \"month\": %s, \"day\": %s, \"type\": %s, \"title\": %s, \"body\": %s, \"source\": %s, \"sourceUrl\": %s, \"storySlug\": %s, \"imageUrl\": %s}%s\n",
                    e.year() != null ? e.year() : "null",
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
