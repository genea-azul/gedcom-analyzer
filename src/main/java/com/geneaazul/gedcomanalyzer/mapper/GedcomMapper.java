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
                .personsCount(gedcom.getPeople().size())
                .modifiedDateTime(gedcom.getModifiedDateTime())
                .reloadDuration(reloadDuration)
                .build();
    }

}
