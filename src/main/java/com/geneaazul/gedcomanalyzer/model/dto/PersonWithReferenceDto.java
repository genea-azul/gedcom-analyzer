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
public class PersonWithReferenceDto {

    @ToString.Include
    private String name;
    @ToString.Include
    private SexType sex;
    @ToString.Include
    private ReferenceType referenceType;

}
