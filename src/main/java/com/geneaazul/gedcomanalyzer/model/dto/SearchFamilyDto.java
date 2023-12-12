package com.geneaazul.gedcomanalyzer.model.dto;

import jakarta.validation.Valid;
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
public class SearchFamilyDto {

    @Valid
    @ToString.Include
    private SearchPersonDto individual;
    @Valid
    @ToString.Include
    private SearchPersonDto spouse;
    @Valid
    @ToString.Include
    private SearchPersonDto father;
    @Valid
    @ToString.Include
    private SearchPersonDto mother;
    @Valid
    @ToString.Include
    private SearchPersonDto paternalGrandfather;
    @Valid
    @ToString.Include
    private SearchPersonDto paternalGrandmother;
    @Valid
    @ToString.Include
    private SearchPersonDto maternalGrandfather;
    @Valid
    @ToString.Include
    private SearchPersonDto maternalGrandmother;
    @Size(max = 180)
    @ToString.Include
    private String contact;
    @Builder.Default
    private Boolean obfuscateLiving = true;
    @Builder.Default
    private Boolean isForceRewrite = false;

}
