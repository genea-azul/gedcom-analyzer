package com.geneaazul.gedcomanalyzer.model.dto;

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
public class SimplePersonDto {

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
    private String profilePicture;
    @ToString.Include
    private String dateOfBirth;
    @ToString.Include
    private String placeOfBirth;
    @ToString.Include
    private String dateOfDeath;

}
