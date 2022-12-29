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
public class SpouseWithChildrenDto {

    @ToString.Include
    private String name;
    @ToString.Include
    private List<String> children;

}
