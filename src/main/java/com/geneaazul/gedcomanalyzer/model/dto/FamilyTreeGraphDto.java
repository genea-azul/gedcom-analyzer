package com.geneaazul.gedcomanalyzer.model.dto;

import java.util.List;

import lombok.Builder;

public record FamilyTreeGraphDto(
        Integer focalPersonId,
        Boolean truncated,
        Integer totalPersons,
        List<PersonNodeDto> persons,
        List<FamilyNodeDto> families) {

    @Builder
    public record PersonNodeDto(
            Integer id,
            String displayName,
            SexType sex,
            String aka,
            String profilePicture,
            Integer yearOfBirth,
            Boolean circaBirth,
            Integer yearOfDeath,
            Boolean circaDeath,
            Boolean isAlive,
            Integer generation,
            String relationship) {
    }

    @Builder
    public record FamilyNodeDto(
            String id,
            List<Integer> husbandIds,
            List<Integer> wifeIds,
            List<Integer> childIds) {
    }

}
