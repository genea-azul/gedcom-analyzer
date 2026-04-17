package com.geneaazul.gedcomanalyzer.model.dto;

import java.util.List;

public record EphemeridesDto(
        List<SimplePersonDto> birthdays,
        List<SimplePersonDto> deaths) {
}
