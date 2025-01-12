package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.domain.SearchConnection;
import com.geneaazul.gedcomanalyzer.model.dto.SearchConnectionDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchConnectionDto;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.CheckForNull;

import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SearchConnectionMapper {

    private final SearchPersonMapper searchPersonMapper;

    @CheckForNull
    @Transactional(propagation = Propagation.MANDATORY)
    public SearchConnection toSearchConnectionEntity(
            SearchConnectionDto searchConnectionDto,
            @Nullable String clientIpAddress) {
        if (isEmpty(searchConnectionDto)) {
            return null;
        }
        return SearchConnection.builder()
                .person1(searchPersonMapper.toSearchPersonSimpleEntity(searchConnectionDto.getPerson1()))
                .person2(searchPersonMapper.toSearchPersonSimpleEntity(searchConnectionDto.getPerson2()))
                .clientIpAddress(clientIpAddress)
                .build();
    }

    @CheckForNull
    @Transactional(propagation = Propagation.MANDATORY)
    public SearchConnectionDetailsDto toSearchConnectionDetailsDto(SearchConnection searchConnection) {
        if (searchConnection == null) {
            return null;
        }
        return SearchConnectionDetailsDto.builder()
                .id(searchConnection.getId())
                .person1(searchPersonMapper.toSearchPersonDto(searchConnection.getPerson1()))
                .person2(searchPersonMapper.toSearchPersonDto(searchConnection.getPerson2()))
                .isMatch(searchConnection.getIsMatch())
                .distance(searchConnection.getDistance())
                .errorMessages(searchConnection.getErrorMessages())
                .isReviewed(searchConnection.getIsReviewed())
                .createDate(searchConnection.getCreateDate())
                .clientIpAddress(searchConnection.getClientIpAddress())
                .build();
    }

    public boolean isEmpty(@Nullable SearchConnectionDto searchConnectionDto) {
        return searchConnectionDto == null
                || searchPersonMapper.isEmpty(searchConnectionDto.getPerson1())
                && searchPersonMapper.isEmpty(searchConnectionDto.getPerson2());
    }

}
