package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.model.SexType;
import com.geneaazul.gedcomanalyzer.utils.SearchUtils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.system.SystemProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
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

    private Path tempDir = Path.of(SystemProperties.get("java.io.tmpdir"));
    private String tempGedcomDirPrefix = "gedcomfile_";

    private boolean deleteUploadedGedcom = false;

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

    private Map<SearchUtils.NameAndSex, String> normalizedNamesMap;
    private Map<String, String> normalizedSurnamesMap;

    @Getter(AccessLevel.NONE)
    private Map<String, List<String>> nameNormalizedM;
    @Getter(AccessLevel.NONE)
    private Map<String, List<String>> nameNormalizedF;
    @Getter(AccessLevel.NONE)
    private Map<String, String> surnameNormalized;

    @PostConstruct
    public void postConstruct() {
        int parentMinAge = alivePersonMaxAge + parentMinAgeDiff;
        int parentMaxAge = alivePersonMaxAge + parentMaxAgeDiff;
        int siblingMaxAge = alivePersonMaxAge + siblingMaxAgeDiff;
        int spouseMinAge = alivePersonMaxAge - spouseMaxAgeDiff;
        int spouseMaxAge = alivePersonMaxAge + spouseMaxAgeDiff;
        int childMaxAge = alivePersonMaxAge - parentMinAgeDiff;

        LocalDate now = LocalDate.now();
        this.parentMinDateOfBirth = now.minusYears(parentMaxAge);
        this.parentMinDateOfDeath = now.minusYears(parentMinAge);
        this.personMinDateOfBirth = now.minusYears(alivePersonMaxAge);
        this.siblingMinDateOfBirth = now.minusYears(siblingMaxAge);
        this.siblingMinDateOfDeath = now.minusYears(siblingMaxAge);
        this.spouseMinDateOfBirth = now.minusYears(spouseMaxAge);
        this.spouseMinDateOfDeath = now.minusYears(spouseMinAge);
        this.childMinDateOfBirth = now.minusYears(childMaxAge);
        this.childMinDateOfDeath = now.minusYears(childMaxAge);

        Map<SearchUtils.NameAndSex, String> m = buildNamesMap(nameNormalizedM, SexType.M);
        Map<SearchUtils.NameAndSex, String> f = buildNamesMap(nameNormalizedF, SexType.F);
        m.putAll(f);

        this.normalizedNamesMap = Map.copyOf(m);
        this.normalizedSurnamesMap = Map.copyOf(surnameNormalized);
    }

    private Map<SearchUtils.NameAndSex, String> buildNamesMap(Map<String, List<String>> normalizedNames, SexType sex) {
        return normalizedNames
                .entrySet()
                .stream()
                .flatMap(entry -> entry
                        .getValue()
                        .stream()
                        .map(name -> new SearchUtils.NameAndSex(name, sex))
                        .map(nameAndSex -> Map.entry(nameAndSex, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
