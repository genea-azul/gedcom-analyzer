package com.geneaazul.gedcomanalyzer.model.dto;

import jakarta.validation.Valid;

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

    @Valid
    @ToString.Include
    private SearchPersonDto person1;
    @Valid
    @ToString.Include
    private SearchPersonDto person2;

}
