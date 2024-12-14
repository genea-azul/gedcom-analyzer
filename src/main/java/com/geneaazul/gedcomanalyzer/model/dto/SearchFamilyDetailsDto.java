package com.geneaazul.gedcomanalyzer.model.dto;

import java.time.OffsetDateTime;

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
public class SearchFamilyDetailsDto {

    @ToString.Include
    private Long id;
    @ToString.Include
    private SearchPersonDto individual;
    @ToString.Include
    private SearchPersonDto spouse;
    @ToString.Include
    private SearchPersonDto father;
    @ToString.Include
    private SearchPersonDto mother;
    @ToString.Include
    private SearchPersonDto paternalGrandfather;
    @ToString.Include
    private SearchPersonDto paternalGrandmother;
    @ToString.Include
    private SearchPersonDto maternalGrandfather;
    @ToString.Include
    private SearchPersonDto maternalGrandmother;
    @ToString.Include
    private Boolean isMatch;
    @ToString.Include
    private Integer potentialResults;
    @ToString.Include
    private String errorMessages;
    @ToString.Include
    private Boolean isReviewed;
    @ToString.Include
    private Boolean isIgnored;
    @ToString.Include
    private Boolean isObfuscated;
    @ToString.Include
    private String markReviewedLink;
    @ToString.Include
    private String markIgnoredLink;
    @ToString.Include
    private String contact;
    @ToString.Include
    private OffsetDateTime createDate;
    @ToString.Include
    private String clientIpAddress;

}
