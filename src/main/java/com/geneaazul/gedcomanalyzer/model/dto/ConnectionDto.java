package com.geneaazul.gedcomanalyzer.model.dto;

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
public class ConnectionDto {

    @ToString.Include
    private String relationship;
    @ToString.Include
    private String personName;
    @ToString.Include
    private String personData;

}
