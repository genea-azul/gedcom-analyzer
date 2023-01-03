package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties
public class GedcomAnalyzerProperties {

    private ZoneId zoneId = ZoneId.of("America/Argentina/Buenos_Aires");

    private Path tempDir = Path.of("../gedcoms/temp");
    private String tempUploadedGedcomDirPrefix = "gedcomfile_";
    private Path localStorageGedcomPath = Path.of("../gedcoms/genea-azul-full-gedcom.ged");
    private String googleStorageBucketName;
    private String googleStorageGedcomBlobName;

    private boolean deleteUploadedGedcom = false;
    private boolean storeFamilySearch = true;

    private int alivePersonMaxAge = 105;
    private int parentMinAgeDiff = 20;
    private int parentMaxAgeDiff = 50;
    private int siblingMaxAgeDiff = 30;
    private int spouseMaxAgeDiff = 15;

    private LocalDate parentMinDateOfBirth;
    private LocalDate parentMinDateOfDeath;
    private LocalDate personMinDateOfBirth;
    private LocalDate siblingMinDateOfBirth;
    private LocalDate siblingMinDateOfDeath;
    private LocalDate spouseMinDateOfBirth;
    private LocalDate spouseMinDateOfDeath;
    private LocalDate childMinDateOfBirth;
    private LocalDate childMinDateOfDeath;

    private Period matchByDayMaxPeriod = Period.ofDays(5);
    private Period matchByMonthMaxPeriod = Period.ofMonths(4);
    private Period matchByYearMaxPeriod = Period.ofYears(3);

    private Map<NameAndSex, String> normalizedNamesMap;
    private Map<String, String> normalizedSurnamesMap;

    @Getter(AccessLevel.NONE)
    private Map<String, List<String>> nameNormalizedM;
    @Getter(AccessLevel.NONE)
    private Map<String, List<String>> nameNormalizedF;
    @Getter(AccessLevel.NONE)
    private Map<String, List<String>> surnameNormalized;

    @PostConstruct
    public void postConstruct() {
        int parentMinAge = alivePersonMaxAge + parentMinAgeDiff;
        int parentMaxAge = alivePersonMaxAge + parentMaxAgeDiff;
        int siblingMaxAge = alivePersonMaxAge + siblingMaxAgeDiff;
        int spouseMinAge = alivePersonMaxAge - spouseMaxAgeDiff;
        int spouseMaxAge = alivePersonMaxAge + spouseMaxAgeDiff;
        int childMaxAge = alivePersonMaxAge - parentMinAgeDiff;

        LocalDate now = LocalDate.now(zoneId);
        this.parentMinDateOfBirth = now.minusYears(parentMaxAge);
        this.parentMinDateOfDeath = now.minusYears(parentMinAge);
        this.personMinDateOfBirth = now.minusYears(alivePersonMaxAge);
        this.siblingMinDateOfBirth = now.minusYears(siblingMaxAge);
        this.siblingMinDateOfDeath = now.minusYears(siblingMaxAge);
        this.spouseMinDateOfBirth = now.minusYears(spouseMaxAge);
        this.spouseMinDateOfDeath = now.minusYears(spouseMinAge);
        this.childMinDateOfBirth = now.minusYears(childMaxAge);
        this.childMinDateOfDeath = now.minusYears(childMaxAge);

        Map<NameAndSex, String> m = invertNamesMap(nameNormalizedM, SexType.M);
        Map<NameAndSex, String> f = invertNamesMap(nameNormalizedF, SexType.F);
        m.putAll(f);

        Map<String, String> s = invertSurnamesMap(surnameNormalized);

        this.normalizedNamesMap = Map.copyOf(m);
        this.normalizedSurnamesMap = Map.copyOf(s);
    }

    private Map<NameAndSex, String> invertNamesMap(Map<String, List<String>> normalizedNames, SexType sex) {
        return normalizedNames
                .entrySet()
                .stream()
                .flatMap(entry -> entry
                        .getValue()
                        .stream()
                        .map(name -> new NameAndSex(name, sex))
                        .map(nameAndSex -> Map.entry(nameAndSex, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, String> invertSurnamesMap(Map<String, List<String>> normalizedSurnames) {
        return normalizedSurnames
                .entrySet()
                .stream()
                .flatMap(entry -> entry
                        .getValue()
                        .stream()
                        .map(surname -> Map.entry(surname, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
