package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SearchFamilyMapper {

    private final SearchPersonMapper searchPersonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public SearchFamily toSearchFamilyEntity(SearchFamilyDto searchFamilyDto) {
        if (isEmpty(searchFamilyDto)) {
            return null;
        }
        return SearchFamily.builder()
                .individualSex(Optional.ofNullable(searchFamilyDto.getIndividual())
                        .map(SearchPersonDto::getSex)
                        .orElse(null))
                .individual(searchPersonMapper.toSearchPersonEntity(searchFamilyDto.getIndividual()))
                .father(searchPersonMapper.toSearchPersonEntity(searchFamilyDto.getFather()))
                .mother(searchPersonMapper.toSearchPersonEntity(searchFamilyDto.getMother()))
                .paternalGrandfather(searchPersonMapper.toSearchPersonEntity(searchFamilyDto.getPaternalGrandfather()))
                .paternalGrandmother(searchPersonMapper.toSearchPersonEntity(searchFamilyDto.getPaternalGrandmother()))
                .maternalGrandfather(searchPersonMapper.toSearchPersonEntity(searchFamilyDto.getMaternalGrandfather()))
                .maternalGrandmother(searchPersonMapper.toSearchPersonEntity(searchFamilyDto.getMaternalGrandmother()))
                .contact(searchFamilyDto.getContact())
                .build();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public SearchFamilyDetailsDto toSearchFamilyDetailsDto(SearchFamily searchFamily) {
        if (searchFamily == null) {
            return null;
        }
        return SearchFamilyDetailsDto.builder()
                .id(searchFamily.getId())
                .individual(searchPersonMapper.toSearchPersonDto(searchFamily.getIndividual(), searchFamily.getIndividualSex()))
                .father(searchPersonMapper.toSearchPersonDto(searchFamily.getFather(), SexType.M))
                .mother(searchPersonMapper.toSearchPersonDto(searchFamily.getMother(), SexType.F))
                .paternalGrandfather(searchPersonMapper.toSearchPersonDto(searchFamily.getPaternalGrandfather(), SexType.M))
                .paternalGrandmother(searchPersonMapper.toSearchPersonDto(searchFamily.getPaternalGrandmother(), SexType.F))
                .maternalGrandfather(searchPersonMapper.toSearchPersonDto(searchFamily.getMaternalGrandfather(), SexType.M))
                .maternalGrandmother(searchPersonMapper.toSearchPersonDto(searchFamily.getMaternalGrandmother(), SexType.F))
                .isMatch(searchFamily.getIsMatch())
                .contact(searchFamily.getContact())
                .createDate(searchFamily.getCreateDate())
                .build();
    }

    public boolean isEmpty(SearchFamilyDto searchFamilyDto) {
        return searchFamilyDto == null
                || searchPersonMapper.isEmpty(searchFamilyDto.getIndividual())
                && searchPersonMapper.isEmpty(searchFamilyDto.getFather())
                && searchPersonMapper.isEmpty(searchFamilyDto.getMother())
                && searchPersonMapper.isEmpty(searchFamilyDto.getPaternalGrandfather())
                && searchPersonMapper.isEmpty(searchFamilyDto.getPaternalGrandmother())
                && searchPersonMapper.isEmpty(searchFamilyDto.getMaternalGrandfather())
                && searchPersonMapper.isEmpty(searchFamilyDto.getMaternalGrandmother())
                && StringUtils.isBlank(searchFamilyDto.getContact());
    }

}
