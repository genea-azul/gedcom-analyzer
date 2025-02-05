package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DateTimeMapper {

    private final GedcomAnalyzerProperties properties;

    public LocalDate toLocalDate(@Nullable OffsetDateTime offsetDateTime) {
        return (offsetDateTime == null)
                ? null
                : offsetDateTime
                        .atZoneSameInstant(properties.getZoneId())
                        .toLocalDate();
    }

}
