package com.geneaazul.gedcomanalyzer.model.dto;

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
public class RelationshipDto {

    @ToString.Include
    private SexType personSex;
    @ToString.Include
    private String personName;
    @ToString.Include
    private ReferenceType referenceType;
    @ToString.Include
    private Integer generation;
    @ToString.Include
    private Integer grade;
    @ToString.Include
    private Boolean isSpouse;
    @ToString.Include
    private Boolean isHalf;

}
