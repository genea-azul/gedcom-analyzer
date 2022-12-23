package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.domain.GivenName;
import com.geneaazul.gedcomanalyzer.model.SexType;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SearchUtils {

    private static final String[] NAME_SEARCH_SPECIAL_CHARS = new String[]{ "?", "(", ")", "'", "-" };
    private static final String[] NAME_REPLACEMENT_SPECIAL_CHARS = new String[]{ "", "", "", "", " " };
    private static final Pattern NAME_MULTIPLE_SPACES_PATTERN = Pattern.compile("  +");

    private static final Pattern SURNAME_COMMON_SUFFIX_PATTERN = Pattern.compile("^([a-z]|de|di|la|lo|mc|mac|saint|sainte) +(.*)$");
    private static final Pattern SURNAME_DOUBLE_LETTERS_PATTERN = Pattern.compile("([a-z])\\1+");
    private static final Pattern SURNAME_VOWELS_ENDING_PATTERN = Pattern.compile("[aeiou]+$");
    private static final String SURNAME_VOWELS_ENDING_REPLACEMENT = "_";
    private static final String[] SURNAME_SEARCH_CHARS = new String[]{ "b", "ç", "je", "ji", "y", "z" };
    private static final String[] SURNAME_REPLACEMENT_CHARS = new String[]{ "v", "c", "ge", "gi", "i", "s" };

    private static final Map<NameAndSex, String> NORMALIZED_NAMES_MAP = Map.<NameAndSex, String>ofEntries(

            /*
             * Masculine
             */

            // Agustín
            Map.entry(NameAndSex.of("agostino", SexType.M), "agustin"), // it
            // Adolfo
            Map.entry(NameAndSex.of("adolphe", SexType.M), "adolfo"), // fr
            // Alejandro
            Map.entry(NameAndSex.of("alessandro", SexType.M), "alejandro"), // it
            // Andrés
            Map.entry(NameAndSex.of("andrea", SexType.M), "andres"), // it
            // Ángel
            Map.entry(NameAndSex.of("angelo", SexType.M), "angel"), // it
            // Antonio
            Map.entry(NameAndSex.of("antonino", SexType.M), "antonio"), // it
            Map.entry(NameAndSex.of("antoine", SexType.M), "antonio"),  // fr
            Map.entry(NameAndSex.of("anthony", SexType.M), "antonio"),  // en
            // Augusto
            Map.entry(NameAndSex.of("auguste", SexType.M), "augusto"), // fr
            // Bautista
            Map.entry(NameAndSex.of("battista", SexType.M), "bautista"), // it
            Map.entry(NameAndSex.of("baptiste", SexType.M), "bautista"), // fr
            // Blas
            Map.entry(NameAndSex.of("biagio", SexType.M), "blas"), // it
            Map.entry(NameAndSex.of("biase", SexType.M), "blas"), // it
            // Camilo
            Map.entry(NameAndSex.of("camillo", SexType.M), "camilo"), // it
            Map.entry(NameAndSex.of("camille", SexType.M), "camilo"), // fr
            // Carlos
            Map.entry(NameAndSex.of("carlo", SexType.M), "carlos"),   // it
            Map.entry(NameAndSex.of("charles", SexType.M), "carlos"), // fr, en
            // Cayetano
            Map.entry(NameAndSex.of("gaetano", SexType.M), "cayetano"), // it
            // Domingo
            Map.entry(NameAndSex.of("domenico", SexType.M), "domingo"),  // it
            Map.entry(NameAndSex.of("dominique", SexType.M), "domingo"), // fr
            // Elías
            Map.entry(NameAndSex.of("elia", SexType.M), "elias"), // it
            // Enrique
            Map.entry(NameAndSex.of("enrico", SexType.M), "enrique"), // it
            Map.entry(NameAndSex.of("henri", SexType.M), "enrique"),  // fr
            Map.entry(NameAndSex.of("henry", SexType.M), "enrique"),  // en
            // Esteban
            Map.entry(NameAndSex.of("stefano", SexType.M), "esteban"), // it
            Map.entry(NameAndSex.of("etienne", SexType.M), "esteban"), // fr
            // Federico
            Map.entry(NameAndSex.of("frederick", SexType.M), "federico"), // en. ge
            // Felipe
            Map.entry(NameAndSex.of("filippo", SexType.M), "felipe"), // it
            Map.entry(NameAndSex.of("phillip", SexType.M), "felipe"), // en
            // Félix
            Map.entry(NameAndSex.of("felice", SexType.M), "felix"), // it
            // Francisco
            Map.entry(NameAndSex.of("francesco", SexType.M), "francisco"), // it
            Map.entry(NameAndSex.of("francis", SexType.M), "francisco"),   // fr
            Map.entry(NameAndSex.of("françois", SexType.M), "francisco"),  // fr
            // Francisco+
            Map.entry(NameAndSex.of("francescantonio", SexType.M), "francisco antonio"), // it
            // Guillermo
            Map.entry(NameAndSex.of("guglielmo", SexType.M), "guillermo"), // it
            Map.entry(NameAndSex.of("guillaume", SexType.M), "guillermo"), // fr
            Map.entry(NameAndSex.of("william", SexType.M), "guillermo"),   // en
            // Hipólito
            Map.entry(NameAndSex.of("ippolito", SexType.M), "hipolito"),  // it
            Map.entry(NameAndSex.of("hippolyte", SexType.M), "hipolito"), // fr
            // Ignacio
            Map.entry(NameAndSex.of("ignazio", SexType.M), "ignacio"), // it
            Map.entry(NameAndSex.of("ignace", SexType.M), "ignacio"),  // fr
            // Joaquín
            Map.entry(NameAndSex.of("gioacchino", SexType.M), "joaquin"), // it
            // Jorge
            Map.entry(NameAndSex.of("giorgio", SexType.M), "jorge"), // it
            Map.entry(NameAndSex.of("george", SexType.M), "jorge"), // en
            // Jose
            Map.entry(NameAndSex.of("giuseppe", SexType.M), "jose"), // it
            Map.entry(NameAndSex.of("joseph", SexType.M), "jose"),   // fr, en
            // Juan
            Map.entry(NameAndSex.of("gio", SexType.M), "juan"),      // it
            Map.entry(NameAndSex.of("giovanni", SexType.M), "juan"), // it
            Map.entry(NameAndSex.of("jean", SexType.M), "juan"),     // fr
            Map.entry(NameAndSex.of("john", SexType.M), "juan"),     // en
            Map.entry(NameAndSex.of("johan", SexType.M), "juan"),    // ge
            Map.entry(NameAndSex.of("johann", SexType.M), "juan"),   // ge
            Map.entry(NameAndSex.of("johannes", SexType.M), "juan"), // ge
            // Juan+
            Map.entry(NameAndSex.of("giambattista", SexType.M), "juan bautista"), // it
            Map.entry(NameAndSex.of("giobattista", SexType.M), "juan bautista"),  // it
            Map.entry(NameAndSex.of("jeanbaptiste", SexType.M), "juan bautista"), // fr
            Map.entry(NameAndSex.of("jeanpierre", SexType.M), "juan pedro"),      // fr
            // Julio
            Map.entry(NameAndSex.of("giulio", SexType.M), "julio"), // it
            Map.entry(NameAndSex.of("jules", SexType.M), "julio"),  // fr
            // Luis
            Map.entry(NameAndSex.of("luigi", SexType.M), "luis"), // it
            Map.entry(NameAndSex.of("louis", SexType.M), "luis"), // fr
            // Manuel
            Map.entry(NameAndSex.of("emanuel", SexType.M), "manuel"),   // es
            Map.entry(NameAndSex.of("emmanuel", SexType.M), "manuel"),  // es
            Map.entry(NameAndSex.of("emanuele", SexType.M), "manuel"),  // it
            Map.entry(NameAndSex.of("emmanuele", SexType.M), "manuel"), // it
            Map.entry(NameAndSex.of("manuele", SexType.M), "manuel"),   // it
            // Marcelino
            Map.entry(NameAndSex.of("marcellin", SexType.M), "marcelino"), // fr
            // Marcelo
            Map.entry(NameAndSex.of("marcel", SexType.M), "marcelo"), // fr
            // Matías
            Map.entry(NameAndSex.of("mathias", SexType.M), "matias"), // en, ge
            // Mateo
            Map.entry(NameAndSex.of("matteo", SexType.M), "mateo"), // it
            Map.entry(NameAndSex.of("matheu", SexType.M), "mateo"), // fr
            Map.entry(NameAndSex.of("matthew", SexType.M), "mateo"), // en
            // Mauricio
            Map.entry(NameAndSex.of("maurizio", SexType.M), "mauricio"), // it
            Map.entry(NameAndSex.of("maurice", SexType.M), "mauricio"),  // fr
            // Miguel
            Map.entry(NameAndSex.of("michele", SexType.M), "miguel"), // it
            Map.entry(NameAndSex.of("michel", SexType.M), "miguel"),  // fr
            Map.entry(NameAndSex.of("michael", SexType.M), "miguel"), // en, ge
            // Miguel+
            Map.entry(NameAndSex.of("michelangelo", SexType.M), "miguel angel"), // it
            // Nicolas
            Map.entry(NameAndSex.of("nicola", SexType.M), "nicolas"),   // it
            Map.entry(NameAndSex.of("nicolo", SexType.M), "nicolas"),   // it
            Map.entry(NameAndSex.of("nicholas", SexType.M), "nicolas"), // en
            Map.entry(NameAndSex.of("nikolaus", SexType.M), "nicolas"), // ge
            // Pablo
            Map.entry(NameAndSex.of("paolo", SexType.M), "pablo"), // it
            Map.entry(NameAndSex.of("paulo", SexType.M), "pablo"), // it
            // Pascual
            Map.entry(NameAndSex.of("pasquale", SexType.M), "pascual"), // it
            // Pedro
            Map.entry(NameAndSex.of("pietro", SexType.M), "pedro"), // it
            Map.entry(NameAndSex.of("pierre", SexType.M), "pedro"), // fr
            Map.entry(NameAndSex.of("peter", SexType.M), "pedro"),  // en
            // Pedro+
            Map.entry(NameAndSex.of("pietrantonio", SexType.M), "pedro antonio"), // it
            // Rafael
            Map.entry(NameAndSex.of("raffaele", SexType.M), "rafael"), // it
            // Renée
            Map.entry(NameAndSex.of("rene", SexType.M), "renee"), // es
            // Ricardo
            Map.entry(NameAndSex.of("richard", SexType.M), "ricardo"), // en
            // Roberto
            Map.entry(NameAndSex.of("robert", SexType.M), "roberto"), // en
            // Santiago
            Map.entry(NameAndSex.of("jacobo", SexType.M), "santiago"),  // es
            Map.entry(NameAndSex.of("giacomo", SexType.M), "santiago"), // it
            Map.entry(NameAndSex.of("jacques", SexType.M), "santiago"), // fr
            Map.entry(NameAndSex.of("james", SexType.M), "santiago"),   // en
            Map.entry(NameAndSex.of("jakob", SexType.M), "santiago"),   // ge
            // Salvador
            Map.entry(NameAndSex.of("salvatore", SexType.M), "salvador"), // it
            // Santos
            Map.entry(NameAndSex.of("santo", SexType.M), "santos"), // it
            // Serafín
            Map.entry(NameAndSex.of("serafino", SexType.M), "serafin"), // it
            // Severo
            Map.entry(NameAndSex.of("saverio", SexType.M), "severo"), // it
            // Simón
            Map.entry(NameAndSex.of("simone", SexType.M), "simon"), // it
            // Tomás
            Map.entry(NameAndSex.of("tommaso", SexType.M), "tomas"), // it
            // Valentín
            Map.entry(NameAndSex.of("valentino", SexType.M), "valentin"), // it
            // Vicente
            Map.entry(NameAndSex.of("vincenzo", SexType.M), "vicente"), // it
            Map.entry(NameAndSex.of("vincent", SexType.M), "vicente"),  // fr, en
            // Víctor
            Map.entry(NameAndSex.of("vittorio", SexType.M), "victor"), // it
            Map.entry(NameAndSex.of("vitto", SexType.M), "victor"),    // it

            /*
             * Feminine
             */

            // Agustina
            Map.entry(NameAndSex.of("agostina", SexType.F), "agustina"), // es, it
            // Alejandra
            Map.entry(NameAndSex.of("alessandra", SexType.F), "alejandra"), // it
            // Alicia
            Map.entry(NameAndSex.of("alice", SexType.F), "alicia"), // en
            // Ana
            Map.entry(NameAndSex.of("anna", SexType.F), "ana"), // it
            Map.entry(NameAndSex.of("anne", SexType.F), "ana"), // fr
            Map.entry(NameAndSex.of("ann", SexType.F), "ana"), // en
            // Antonela
            Map.entry(NameAndSex.of("antonella", SexType.F), "antonela"), // es, it
            // Antonia
            Map.entry(NameAndSex.of("antonina", SexType.F), "antonia"), // it
            Map.entry(NameAndSex.of("antonie", SexType.F), "antonia"), // fr
            // Antonieta
            Map.entry(NameAndSex.of("antonietta", SexType.F), "antonieta"), // it
            Map.entry(NameAndSex.of("antoinette", SexType.F), "antonieta"), // fr
            // Anunciada
            Map.entry(NameAndSex.of("nunciada", SexType.F), "anunciada"),   // es
            Map.entry(NameAndSex.of("annunziata", SexType.F), "anunciada"), // it
            Map.entry(NameAndSex.of("nunziata", SexType.F), "anunciada"),   // it
            // Asunción
            Map.entry(NameAndSex.of("asunta", SexType.F), "asuncion"),     // es
            Map.entry(NameAndSex.of("assunzione", SexType.F), "asuncion"), // it
            Map.entry(NameAndSex.of("assunta", SexType.F), "asuncion"),    // it
            // Beatriz
            Map.entry(NameAndSex.of("beatrice", SexType.F), "beatriz"), // it
            // Camila
            Map.entry(NameAndSex.of("camilla", SexType.F), "camila"), // it
            Map.entry(NameAndSex.of("camille", SexType.F), "camila"), // fr
            // Catalina
            Map.entry(NameAndSex.of("catarina", SexType.F), "catalina"),  // it
            Map.entry(NameAndSex.of("cattarina", SexType.F), "catalina"), // it
            Map.entry(NameAndSex.of("caterina", SexType.F), "catalina"),  // it
            Map.entry(NameAndSex.of("catterina", SexType.F), "catalina"), // it
            Map.entry(NameAndSex.of("catherina", SexType.F), "catalina"), // fr
            Map.entry(NameAndSex.of("catherine", SexType.F), "catalina"), // fr, en
            Map.entry(NameAndSex.of("katharina", SexType.F), "catalina"), // ge
            Map.entry(NameAndSex.of("katherina", SexType.F), "catalina"), // ge
            // Carmen
            Map.entry(NameAndSex.of("carmela", SexType.F), "carmen"), // it
            // Carola
            Map.entry(NameAndSex.of("carole", SexType.F), "carola"), // en
            // Carolina
            Map.entry(NameAndSex.of("caroline", SexType.F), "carolina"), // fr
            // Cayetana
            Map.entry(NameAndSex.of("gaetana", SexType.F), "cayetana"), // it
            // Concepción
            Map.entry(NameAndSex.of("concezione", SexType.F), "concepcion"), // it
            // Dominga
            Map.entry(NameAndSex.of("domenica", SexType.F), "dominga"),  // it
            Map.entry(NameAndSex.of("dominica", SexType.F), "dominga"),  // fr
            Map.entry(NameAndSex.of("dominique", SexType.F), "dominga"), // fr
            // Elba
            Map.entry(NameAndSex.of("elva", SexType.F), "elba"), // es
            // Elisa
            Map.entry(NameAndSex.of("elisabet", SexType.F), "elisa"),   // es
            Map.entry(NameAndSex.of("elisabeth", SexType.F), "elisa"),  // es
            Map.entry(NameAndSex.of("elisabetta", SexType.F), "elisa"), // it
            Map.entry(NameAndSex.of("elizabet", SexType.F), "elisa"),   // en
            Map.entry(NameAndSex.of("elizabeth", SexType.F), "elisa"),  // en
            Map.entry(NameAndSex.of("eliza", SexType.F), "elisa"),      // en
            // Leonor
            Map.entry(NameAndSex.of("eleonora", SexType.F), "leonor"), // it
            // Emma
            Map.entry(NameAndSex.of("ema", SexType.F), "emma"), // es
            // Enriqueta
            Map.entry(NameAndSex.of("enrichetta", SexType.F), "enriqueta"), // it
            Map.entry(NameAndSex.of("henrietta", SexType.F), "enriqueta"),  // ge
            // Esther
            Map.entry(NameAndSex.of("ester", SexType.F), "esther"), // es
            // Felipa
            Map.entry(NameAndSex.of("filippa", SexType.F), "felipa"), // it
            // Fiorella
            Map.entry(NameAndSex.of("fiorela", SexType.F), "fiorella"), // es, it
            // Francisca
            Map.entry(NameAndSex.of("francesca", SexType.F), "francisca"), // it
            Map.entry(NameAndSex.of("françoise", SexType.F), "francisca"), // fr
            // Francisca+
            Map.entry(NameAndSex.of("francescantonia", SexType.F), "francisca antonia"), // it
            // Gladys
            Map.entry(NameAndSex.of("gladis", SexType.F), "gladys"), // es
            // Graciana
            Map.entry(NameAndSex.of("gratianne", SexType.F), "graciana"), // it
            // Guillermina
            Map.entry(NameAndSex.of("guglielmina", SexType.F), "guillermina"), // it
            // Haydée
            Map.entry(NameAndSex.of("aide", SexType.F), "haydee"),  // es
            Map.entry(NameAndSex.of("aidee", SexType.F), "haydee"), // es
            // Hilda
            Map.entry(NameAndSex.of("ilda", SexType.F), "hilda"),  // es
            // Hipólita
            Map.entry(NameAndSex.of("ippolita", SexType.F), "hipolita"), // it
            // Ignacia
            Map.entry(NameAndSex.of("ignazia", SexType.F), "ignacia"), // it
            // Inés
            Map.entry(NameAndSex.of("agnese", SexType.F), "ines"), // it
            // Isabel
            Map.entry(NameAndSex.of("isabela", SexType.F), "isabel"),  // es
            Map.entry(NameAndSex.of("isabella", SexType.F), "isabel"), // it
            // Joaquina
            Map.entry(NameAndSex.of("gioacchina", SexType.F), "joaquina"), // it
            // Josefa
            Map.entry(NameAndSex.of("giuseppa", SexType.F), "josefa"), // it
            Map.entry(NameAndSex.of("josephte", SexType.F), "josefa"), // fr
            // Josefina
            Map.entry(NameAndSex.of("giuseppina", SexType.F), "josefina"), // it
            // Juana
            Map.entry(NameAndSex.of("giovanna", SexType.F), "juana"), // it
            Map.entry(NameAndSex.of("jeanne", SexType.F), "juana"),   // fr
            // Julia
            Map.entry(NameAndSex.of("giulia", SexType.F), "julia"), // it
            Map.entry(NameAndSex.of("julie", SexType.F), "julia"),  // fr
            // Luisa
            Map.entry(NameAndSex.of("luigia", SexType.F), "luisa"), // it
            Map.entry(NameAndSex.of("louise", SexType.F), "luisa"), // fr
            // Margarita
            Map.entry(NameAndSex.of("margherita", SexType.F), "margarita"),  // it
            Map.entry(NameAndSex.of("marguerite", SexType.F), "margarita"),  // fr
            Map.entry(NameAndSex.of("margueritte", SexType.F), "margarita"), // fr
            Map.entry(NameAndSex.of("margaret", SexType.F), "margarita"),    // en
            Map.entry(NameAndSex.of("margarete", SexType.F), "margarita"),   // ge
            Map.entry(NameAndSex.of("margrethe", SexType.F), "margarita"),   // ge
            // María
            Map.entry(NameAndSex.of("marie", SexType.F), "maria"), // fr
            Map.entry(NameAndSex.of("mary", SexType.F), "maria"),  // en
            // María+
            Map.entry(NameAndSex.of("mariangela", SexType.F), "maria angela"),   // it
            Map.entry(NameAndSex.of("mariangiola", SexType.F), "maria angela"),  // it
            Map.entry(NameAndSex.of("mariantonia", SexType.F), "maria antonia"), // it
            // Mariana
            Map.entry(NameAndSex.of("marianna", SexType.F), "mariana"), // it
            Map.entry(NameAndSex.of("marianne", SexType.F), "mariana"), // fr
            // Martha
            Map.entry(NameAndSex.of("marta", SexType.F), "martha"), // es
            // Matea
            Map.entry(NameAndSex.of("mattea", SexType.F), "matea"), // it
            // Miguela
            Map.entry(NameAndSex.of("michela", SexType.F), "miguela"),  // it
            Map.entry(NameAndSex.of("michelle", SexType.F), "miguela"), // fr
            Map.entry(NameAndSex.of("michele", SexType.F), "miguela"),  // fr
            Map.entry(NameAndSex.of("michaela", SexType.F), "miguela"), // en, ge
            // Mirtha
            Map.entry(NameAndSex.of("mirta", SexType.F), "mirtha"), // es
            // Nicolasa
            Map.entry(NameAndSex.of("nicolina", SexType.F), "nicolasa"), // it
            // Pascuala
            Map.entry(NameAndSex.of("pasquala", SexType.F), "pascuala"), // it
            // Paulina
            Map.entry(NameAndSex.of("paolina", SexType.F), "paulina"), // it
            // Rafaela
            Map.entry(NameAndSex.of("raffaela", SexType.F), "rafaela"), // it
            // Raquel
            Map.entry(NameAndSex.of("rachela", SexType.F), "raquel"),  // it
            Map.entry(NameAndSex.of("rachele", SexType.F), "raquel"),  // it
            Map.entry(NameAndSex.of("rachel", SexType.F), "raquel"),   // fr, en, ge
            Map.entry(NameAndSex.of("rachelle", SexType.F), "raquel"), // fr
            // Renée
            Map.entry(NameAndSex.of("rene", SexType.F), "renee"), // es
            // Rosario
            Map.entry(NameAndSex.of("rosaria", SexType.F), "rosario"), // it
            // Salvadora
            Map.entry(NameAndSex.of("salvatora", SexType.F), "salvadora"),  // it
            Map.entry(NameAndSex.of("salvatrice", SexType.F), "salvadora"), // it
            // Severa
            Map.entry(NameAndSex.of("saveria", SexType.F), "severa"), // it
            // Tomasa
            Map.entry(NameAndSex.of("tommasa", SexType.F), "tomasa"), // it
            // Vanesa
            Map.entry(NameAndSex.of("vanessa", SexType.F), "vanesa"), // es
            // Vicenta
            Map.entry(NameAndSex.of("vincenza", SexType.F), "vicenta"), // it
            // Victoria
            Map.entry(NameAndSex.of("vittoria", SexType.F), "victoria"), // it
            // Viviana
            Map.entry(NameAndSex.of("bibiana", SexType.F), "viviana") // es
    );

    // Attention: surnames should not contain double letters, see usage of this map in 'normalizeSurname()' method
    private static final Map<String, String> NORMALIZED_SURNAMES_MAP = Map.ofEntries(
            Map.entry("chiriglian_", "ciriglian_"),
            Map.entry("lacamer_", "lacamar_"),
            Map.entry("ponphil_", "pomphil_")
    );

    public static String simplifyName(String name) {
        name = StringUtils.stripAccents(name);
        name = StringUtils.lowerCase(name);
        name = StringUtils.replaceEach(name, NAME_SEARCH_SPECIAL_CHARS, NAME_REPLACEMENT_SPECIAL_CHARS);
        name = RegExUtils.replaceAll(name, NAME_MULTIPLE_SPACES_PATTERN, " ");
        name = StringUtils.trim(name);
        return name;
    }

    public static boolean matchesGivenName(GivenName name1, GivenName name2) {

        if (name1.getWordsCount() == 1 && name2.getWordsCount() == 1) {
            return name1.getName().equals(name2.getName());
        }

        if (name1.getWordsCount() <= name2.getWordsCount()) {
            // In case name1 is whole-word, make a whole-word search in name2
            return name1.getSearchPattern().matcher(name2.getName()).find();
        } else {
            // In case name2 is whole-word, make a whole-word search in name1
            return name2.getSearchPattern().matcher(name1.getName()).find();
        }
    }

    public static String normalizeName(String name, SexType sex) {
        String[] words = StringUtils.splitByWholeSeparator(name, " ");
        return Arrays.stream(words)
                .map(word -> Optional.ofNullable(NORMALIZED_NAMES_MAP.get(NameAndSex.of(word, sex)))
                        .orElse(word))
                .collect(Collectors.joining(" "));
    }

    /**
     * di yannibelli rago  ->  diyannibelli rago   (concat common prefix)
     *  diyannibelli rago  ->  diyannibelli        (consider only first word)
     *       diyannivelli  ->  diiannivelli        (replace b with v, replace y with i)
     *       diiannivelli  ->  dianiveli           (remove repeated letters)
     *          dianiveli  ->  dianivel_           (replace last vowels with a _)
     *          dianivel_  ->  ciamivel_           (get optional replacement from NORMALIZED_SURNAMES_MAP)
     */
    public static String normalizeSurname(String surname) {
        surname = RegExUtils.replaceAll(surname, SURNAME_COMMON_SUFFIX_PATTERN, "$1$2");
        surname = StringUtils.substringBefore(surname, " ");
        surname = StringUtils.replaceEach(surname, SURNAME_SEARCH_CHARS, SURNAME_REPLACEMENT_CHARS);
        surname = RegExUtils.replaceAll(surname, SURNAME_DOUBLE_LETTERS_PATTERN, "$1");
        surname = RegExUtils.replaceAll(surname, SURNAME_VOWELS_ENDING_PATTERN, SURNAME_VOWELS_ENDING_REPLACEMENT);
        surname = Optional.ofNullable(surname)
                .map(NORMALIZED_SURNAMES_MAP::get)
                .orElse(surname);
        return surname;
    }

    @Value
    @RequiredArgsConstructor(staticName = "of")
    @SuppressWarnings("RedundantModifiersValueLombok")
    public static class NameAndSex {
        private final String name;
        private final SexType sex;
    }

}
