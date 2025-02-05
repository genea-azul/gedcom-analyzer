package com.geneaazul.gedcomanalyzer.model.dto;

import java.time.LocalDate;
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
public class ClientStatsDto {

    @ToString.Include
    private String ipAddress;

    @ToString.Include
    private Integer searchesCount;

    @ToString.Include
    private LocalDate firstSearch;

    @ToString.Include
    private LocalDate lastSearch;

    @ToString.Include
    private List<String> topSurnames;

}
