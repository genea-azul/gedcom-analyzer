package com.geneaazul.gedcomanalyzer.domain;

import com.geneaazul.gedcomanalyzer.model.SexType;

import java.time.Year;

public record NameSexYear(String name, SexType sex, Year year) {
}
