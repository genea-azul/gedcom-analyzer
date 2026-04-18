package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomMetadataDto;

import org.springframework.stereotype.Component;

import java.time.Duration;

import jakarta.annotation.Nullable;

@Component
public class GedcomMapper {

    public GedcomMetadataDto toGedcomMetadataDto(
            EnrichedGedcom gedcom,
            @Nullable Duration reloadDuration) {
        return GedcomMetadataDto.builder()
                /* General */
                .personsCount(gedcom.getPeople().size())
                .familiesCount(gedcom.getFamiliesCount())
                .maleCount(gedcom.getMaleCount())
                .femaleCount(gedcom.getFemaleCount())
                .aliveCount(gedcom.getAliveCount())
                .deceasedCount(gedcom.getDeceasedCount())
                .distinguishedCount(gedcom.getDistinguishedCount())
                .nativeCount(gedcom.getNativeCount())
                /* Azul specific */
                .azulPersonsCount(gedcom.getAzulPersonsCount())
                .azulAliveCount(gedcom.getAzulAliveCount())
                .azulSurnamesCount(gedcom.getAzulSurnamesCount())
                .azulMayorsCount(gedcom.getAzulMayorsCount())
                .azulDisappearedCount(gedcom.getAzulDisappearedCount())
                /* Timing */
                .modifiedDateTime(gedcom.getModifiedDateTime())
                .reloadDuration(reloadDuration)
                .build();
    }

}
