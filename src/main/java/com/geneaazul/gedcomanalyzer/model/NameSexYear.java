package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import java.time.Year;

public record NameSexYear(String name, SexType sex, Year year) {
}
