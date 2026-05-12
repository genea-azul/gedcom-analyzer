package com.geneaazul.gedcomanalyzer.model.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class TreeBuilderSubmitDto {

    @Valid
    @NotNull
    @ToString.Include
    private TreeBuilderPersonDto ego;

    @Valid
    private TreeBuilderPersonDto partner;

    @Valid
    private TreeBuilderPersonDto father;

    @Valid
    private TreeBuilderPersonDto mother;

    @Valid
    private TreeBuilderPersonDto paternalGrandfather;

    @Valid
    private TreeBuilderPersonDto paternalGrandmother;

    @Valid
    private TreeBuilderPersonDto maternalGrandfather;

    @Valid
    private TreeBuilderPersonDto maternalGrandmother;

    @Valid
    @Size(max = 20)
    private List<TreeBuilderPersonDto> children;

    @Size(max = 120)
    @ToString.Include
    private String contact;

}
