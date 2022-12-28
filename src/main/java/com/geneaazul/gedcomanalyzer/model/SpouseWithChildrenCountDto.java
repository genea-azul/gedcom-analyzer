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
public class SpouseWithChildrenCountDto {

    @ToString.Include
    private String displayName;
    @ToString.Include
    private Integer childrenCount;

}
