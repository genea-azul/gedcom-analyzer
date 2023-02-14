package com.geneaazul.gedcomanalyzer.model.dto;

import java.util.List;

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
public class SearchFamilyResultDto {

    @ToString.Include
    @Builder.Default
    private List<PersonDto> people = List.of();

    @ToString.Include
    @Builder.Default
    private Integer potentialResults = 0;

    @ToString.Include
    @Builder.Default
    private List<String> errors = List.of();

}
