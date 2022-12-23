package com.geneaazul.gedcomanalyzer.model;

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
public class PersonDuplicateCompareDto {

    @ToString.Include
    private PersonDto person;
    @ToString.Include
    private Integer score;

}
