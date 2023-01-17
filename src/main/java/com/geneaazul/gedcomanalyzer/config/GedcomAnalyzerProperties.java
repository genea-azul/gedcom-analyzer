package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.utils.SearchUtils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

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

    private Map<NameAndSex, String> normalizedGivenNamesMap;
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

        this.normalizedGivenNamesMap = SearchUtils.invertGivenNamesMap(nameNormalizedM, nameNormalizedF);
        this.normalizedSurnamesMap = SearchUtils.invertSurnamesMap(surnameNormalized);
    }

}
