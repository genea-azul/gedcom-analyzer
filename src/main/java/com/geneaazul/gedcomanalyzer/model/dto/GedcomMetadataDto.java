package com.geneaazul.gedcomanalyzer.model.dto;

import java.time.Duration;
import java.time.ZonedDateTime;

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
public class GedcomMetadataDto {

    /* General */

    @ToString.Include
    private Integer personsCount;

    @ToString.Include
    private Integer familiesCount;

    @ToString.Include
    private Integer maleCount;

    @ToString.Include
    private Integer femaleCount;

    @ToString.Include
    private Integer aliveCount;

    @ToString.Include
    private Integer deceasedCount;

    @ToString.Include
    private Integer distinguishedCount;

    @ToString.Include
    private Integer nativeCount;

    /* Azul specific */

    @ToString.Include
    private Integer azulPersonsCount;

    @ToString.Include
    private Integer azulAliveCount;

    @ToString.Include
    private Integer azulSurnamesCount;

    @ToString.Include
    private Integer azulMayorsCount;

    @ToString.Include
    private Integer azulDisappearedCount;

    /* Timing */

    @ToString.Include
    private ZonedDateTime modifiedDateTime;

    @ToString.Include
    private Duration reloadDuration;

}
