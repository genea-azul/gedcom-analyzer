package com.geneaazul.gedcomanalyzer.model.dto;

import java.util.List;

public enum SexType {

    F,
    M,
    U; // Undefined

    public static final List<SexType> VALID_SEX_TYPES = List.of(F, M);

}
