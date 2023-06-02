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
public class SearchSurnameResultDto {

    @ToString.Include
    private String surname;

    @ToString.Include
    @Builder.Default
    private Integer frequency = 0;

    @ToString.Include
    @Builder.Default
    private List<String> variants = List.of();

    @ToString.Include
    @Builder.Default
    private List<String> countries = List.of();

    @ToString.Include
    private Integer firstSeenYear;

    @ToString.Include
    private Integer lastSeenYear;

}
