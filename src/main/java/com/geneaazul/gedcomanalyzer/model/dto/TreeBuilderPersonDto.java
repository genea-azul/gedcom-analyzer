package com.geneaazul.gedcomanalyzer.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

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
public class TreeBuilderPersonDto {

    @Size(max = 60)
    @ToString.Include
    private String givenName;

    @Size(max = 60)
    @ToString.Include
    private String surname;

    @ToString.Include
    private SexType sex;

    @Min(1)
    @Max(31)
    @ToString.Include
    private Integer birthDay;

    @Min(1)
    @Max(12)
    @ToString.Include
    private Integer birthMonth;

    @Min(0)
    @Max(2100)
    @ToString.Include
    private Integer birthYear;

    @Size(max = 80)
    @ToString.Include
    private String birthPlace;

    @ToString.Include
    private Boolean isDeceased;

    @Min(1)
    @Max(31)
    @ToString.Include
    private Integer deathDay;

    @Min(1)
    @Max(12)
    @ToString.Include
    private Integer deathMonth;

    @Min(0)
    @Max(2100)
    @ToString.Include
    private Integer deathYear;

    @Size(max = 80)
    @ToString.Include
    private String deathPlace;

    /** Only meaningful for the partner. */
    @ToString.Include
    private TreeBuilderRelationshipType relationshipType;

}
