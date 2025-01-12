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
public class SearchConnectionDetailsDto {

    @ToString.Include
    private Long id;
    @ToString.Include
    private SearchPersonDto person1;
    @ToString.Include
    private SearchPersonDto person2;
    @ToString.Include
    private Boolean isMatch;
    @ToString.Include
    private Integer distance;
    @ToString.Include
    private String errorMessages;
    @ToString.Include
    private Boolean isReviewed;
    @ToString.Include
    private String markReviewedLink;
    @ToString.Include
    private String contact;
    @ToString.Include
    private OffsetDateTime createDate;
    @ToString.Include
    private String clientIpAddress;

}
