package com.geneaazul.gedcomanalyzer.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class GedcomAnalysisDto {

    @ToString.Include
    private Integer personsCount;
    @ToString.Include
    private Integer familiesCount;
    @ToString.Include
    private List<PersonDuplicateDto> personDuplicates;
    @ToString.Include
    private List<PersonDto> invalidAlivePersons;

}
