package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomMetadataDto;

import org.springframework.stereotype.Component;

@Component
public class GedcomMapper {

    public GedcomMetadataDto toGedcomMetadataDto(EnrichedGedcom gedcom) {
        return GedcomMetadataDto.builder()
                .personsCount(gedcom.getPeople().size())
                .modifiedDateTime(gedcom.getModifiedDateTime())
                .build();
    }

}
