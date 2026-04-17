package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.PersonMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.dto.SimplePersonDto;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BirthdayService {

    private final PersonMapper personMapper;

    /**
     * Returns alive persons linked to Azul who were born on the given date's month and day.
     * The result is derived from a pre-computed index in {@link EnrichedGedcom}, rebuilt on
     * every gedcom reload, making this a O(1) lookup followed by lightweight DTO mapping.
     */
    public List<SimplePersonDto> getBirthdaysInAzulToday(EnrichedGedcom gedcom, LocalDate date) {
        MonthDay today = MonthDay.from(date);
        List<EnrichedPerson> birthdayPeople = gedcom.getAzulAlivePersonsByBirthdayIndex()
                .getOrDefault(today, List.of());
        return personMapper.toSimplePersonDto(birthdayPeople, true);
    }

}
