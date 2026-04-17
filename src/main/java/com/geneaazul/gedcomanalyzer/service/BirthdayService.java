package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.PersonMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.dto.EphemeridesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SimplePersonDto;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
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
        List<EnrichedPerson> birthdayPeople = new ArrayList<>(
                gedcom.getAzulAlivePersonsByBirthdayIndex().getOrDefault(today, List.of()));
        Collections.shuffle(birthdayPeople);
        return personMapper.toSimplePersonDto(birthdayPeople, true);
    }

    /**
     * Returns an efemérides snapshot for the entire month of the given date:
     * <ul>
     *   <li>{@link EphemeridesDto#birthdays()} — living distinguished persons born in this month, ordered by day.</li>
     *   <li>{@link EphemeridesDto#deaths()} — deceased distinguished persons who died in this month, ordered by day.</li>
     * </ul>
     * Built from the pre-computed {@code MonthDay} indexes in {@link EnrichedGedcom} via a short
     * day-by-day loop (28–31 iterations), so the cost is negligible. No obfuscation is applied
     * since personalities are public figures.
     */
    public EphemeridesDto getEphemeridesThisMonth(EnrichedGedcom gedcom, LocalDate date) {
        Month month = date.getMonth();
        int daysInMonth = month.length(Year.isLeap(date.getYear()));

        List<EnrichedPerson> birthdayPeople = new ArrayList<>();
        List<EnrichedPerson> deathPeople = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            MonthDay monthDay = MonthDay.of(month, day);
            birthdayPeople.addAll(gedcom.getDistinguishedPersonsByBirthdayIndex().getOrDefault(monthDay, List.of()));
            deathPeople.addAll(gedcom.getDistinguishedPersonsByDeathdayIndex().getOrDefault(monthDay, List.of()));
        }

        return new EphemeridesDto(
                personMapper.toSimplePersonDto(birthdayPeople, false),
                personMapper.toSimplePersonDto(deathPeople, false));
    }

}
