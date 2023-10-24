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
public class GedcomAnalysisDto {

    @ToString.Include
    private Integer personsCount;
    @ToString.Include
    private List<PersonDuplicateDto> personDuplicates;
    @ToString.Include
    private List<PersonDto> invalidAlivePersons;

}
