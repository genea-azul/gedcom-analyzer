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
public class PersonDuplicateDto {

    @ToString.Include
    private PersonDto person;
    @ToString.Include
    private List<PersonDuplicateCompareDto> duplicates;

}
