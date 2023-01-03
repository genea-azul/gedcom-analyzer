package com.geneaazul.gedcomanalyzer.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class SearchPersonDto {

    @Size(max = 60)
    @ToString.Include
    private String givenName;
    @Size(max = 60)
    @ToString.Include
    private String surname;
    @ToString.Include
    private SexType sex;
    @ToString.Include
    private Boolean isAlive;
    @Min(0)
    @Max(2050)
    @ToString.Include
    private Integer yearOfBirth;
    @Min(0)
    @Max(2050)
    @ToString.Include
    private Integer yearOfDeath;

}
