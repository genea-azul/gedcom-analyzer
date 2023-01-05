package com.geneaazul.gedcomanalyzer.model.dto;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
    private String contact;
    @ToString.Include
    private OffsetDateTime createDate;

}
