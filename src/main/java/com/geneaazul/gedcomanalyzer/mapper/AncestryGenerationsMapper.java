package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.dto.AncestryGenerationsDto;

import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;

@Component
public class AncestryGenerationsMapper {

    public AncestryGenerationsDto toAncestryGenerationDto(
            @Nullable AncestryGenerations ancestryGenerations) {

        if (ancestryGenerations == null) {
            return null;
        }

        return AncestryGenerationsDto.builder()
                .ascending(ancestryGenerations.ascending())
                .descending(ancestryGenerations.descending())
                .directDescending(ancestryGenerations.directDescending())
                .build();
    }

}
