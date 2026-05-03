package com.geneaazul.gedcomanalyzer.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
public class SearchConnectionDto {

    @NotNull
    @Valid
    @ToString.Include
    private SearchPersonDto person1;
    @NotNull
    @Valid
    @ToString.Include
    private SearchPersonDto person2;

}
