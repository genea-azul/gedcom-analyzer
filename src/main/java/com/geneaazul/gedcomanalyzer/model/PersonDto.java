package com.geneaazul.gedcomanalyzer.model;

import org.apache.commons.lang3.tuple.Pair;

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
public class PersonDto {

    @ToString.Include
    private String id;
    @ToString.Include
    private SexType sex;
    @ToString.Include
    private boolean alive;
    @ToString.Include
    private String displayName;
    private List<String> parents;
    private List<Pair<String, Integer>> spouses;

}
