package com.geneaazul.gedcomanalyzer.model.dto;

import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class PersonDto {

    @ToString.Include
    private UUID uuid;
    @ToString.Include
    private SexType sex;
    @ToString.Include
    private Boolean isAlive;
    @ToString.Include
    private String name;
    @ToString.Include
    private String aka;
    @ToString.Include
    private String dateOfBirth;
    @ToString.Include
    private String placeOfBirth;
    @ToString.Include
    private String dateOfDeath;
    private List<PersonWithReferenceDto> parents;
    private List<SpouseWithChildrenDto> spouses;
    @ToString.Include
    private Integer personsCountInTree;
    @ToString.Include
    private Integer surnamesCountInTree;
    private List<String> ancestryCountries;
    @ToString.Include
    private AncestryGenerationsDto ancestryGenerations;
    @ToString.Include
    private RelationshipDto maxDistantRelationship;
    private List<String> distinguishedPersonsInTree;

}
