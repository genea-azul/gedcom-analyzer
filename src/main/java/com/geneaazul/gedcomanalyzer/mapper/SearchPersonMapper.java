package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.domain.SearchPerson;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import javax.annotation.CheckForNull;

@Component
@RequiredArgsConstructor
public class SearchPersonMapper {

    @CheckForNull
    @Transactional(propagation = Propagation.MANDATORY)
    public SearchPerson toSearchPersonEntity(SearchPersonDto searchPersonDto) {
        if (isEmpty(searchPersonDto)) {
            return null;
        }
        return SearchPerson.builder()
                .givenName(searchPersonDto.getGivenName())
                .surname(searchPersonDto.getSurname())
                .isAlive(searchPersonDto.getIsAlive())
                .yearOfBirth(searchPersonDto.getYearOfBirth())
                .yearOfDeath(searchPersonDto.getYearOfDeath())
                .placeOfBirth(searchPersonDto.getPlaceOfBirth())
                .build();
    }

    @CheckForNull
    @Transactional(propagation = Propagation.MANDATORY)
    public SearchPersonDto toSearchPersonDto(@Nullable SearchPerson searchPerson, SexType sex) {
        if (searchPerson == null) {
            return null;
        }
        return SearchPersonDto.builder()
                .givenName(searchPerson.getGivenName())
                .surname(searchPerson.getSurname())
                .sex(sex)
                .isAlive(searchPerson.getIsAlive())
                .yearOfBirth(searchPerson.getYearOfBirth())
                .yearOfDeath(searchPerson.getYearOfDeath())
                .placeOfBirth(searchPerson.getPlaceOfBirth())
                .build();
    }

    public boolean isEmpty(@Nullable SearchPersonDto searchPersonDto) {
        return searchPersonDto == null
                || StringUtils.isBlank(searchPersonDto.getGivenName())
                && StringUtils.isBlank(searchPersonDto.getSurname())
                && searchPersonDto.getYearOfBirth() == null
                && searchPersonDto.getYearOfDeath() == null;
    }

}
