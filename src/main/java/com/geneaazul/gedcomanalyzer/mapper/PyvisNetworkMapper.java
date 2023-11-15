package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Surname;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PyvisNetworkMapper {

    private final RelationshipMapper relationshipMapper;

    public String[] toPyvisNodeCsvRecord(
            EnrichedPerson person,
            Map<String, String[]> countryColorsMap,
            String defaultLabel,
            String[] defaultColors,
            double size) {
        return new String[] {
                person.getId(),
                getPyvisNodeLabel(person, defaultLabel),
                getPyvisNodeTitle(person),
                person.getPlaceOfBirth()
                        .map(Place::country)
                        .map(countryColorsMap::get)
                        .map(countryColors -> person.isAlive()
                                ? countryColors[0]
                                : countryColors[1])
                        .orElseGet(() -> person.isAlive()
                                ? defaultColors[0]
                                : defaultColors[1]),
                String.valueOf(size)
        };
    }

    private String getPyvisNodeLabel(EnrichedPerson person, String defaultLabel) {
        String label = Stream
                .of(
                        person.getGivenName()
                                .map(GivenName::value),
                        person.getSurname()
                                .map(Surname::value))
                .flatMap(Optional::stream)
                .collect(Collectors.joining(" "));

        return StringUtils.defaultIfEmpty(label, defaultLabel);
    }

    private String getPyvisNodeTitle(EnrichedPerson person) {
        String title = relationshipMapper
                .displayNameInSpanish(person.getDisplayName());
        if (person.getDateOfBirth().isPresent()) {
            title = title + " - " + person.getDateOfBirth().get().getYear();
        }
        if (person.getPlaceOfBirth().isPresent()) {
            title = title + " - " + person.getPlaceOfBirth().get().country();
        }
        return title;
    }

    public String[] toPyvisCoupleNodeCsvRecord(
            EnrichedPerson person,
            EnrichedPerson spouse,
            boolean noChildren,
            String defaultLabel,
            String color,
            double size) {
        String nodeId = buildCoupleNodeId(person, spouse, noChildren);
        String personSurname = person.getSurname()
                .map(Surname::value)
                .orElse(defaultLabel);
        String spouseSurname = spouse.getSurname()
                .map(Surname::value)
                .orElse(defaultLabel);
        return new String[] {
                nodeId,
                personSurname + " - " + spouseSurname,
                person.getDisplayName() + " - " + spouse.getDisplayName(),
                color,
                String.valueOf(size)
        };
    }

    public String[] toPyvisSpouseEdgeCsvRecord(
            EnrichedPerson person,
            EnrichedPerson spouse,
            boolean noChildren,
            boolean separated,
            String separatedTitle,
            double weight,
            double width) {
        String coupleNodeId = buildCoupleNodeId(person, spouse, noChildren);
        return new String[] {
                person.getId(),
                coupleNodeId,
                separated
                        ? separatedTitle
                        : null,
                String.valueOf(weight),
                String.valueOf(width)
        };
    }

    public String[] toPyvisChildEdgeCsvRecord(
            String sourceId,
            String childId,
            boolean adopted,
            String adoptedTitle,
            double weight,
            double width) {
        return new String[] {
                sourceId,
                childId,
                adopted
                        ? adoptedTitle
                        : null,
                String.valueOf(weight),
                String.valueOf(width)
        };
    }

    public String buildCoupleNodeId(EnrichedPerson person, EnrichedPerson spouse, boolean noChildren) {
        if (noChildren) {
            return spouse.getId();
        }

        return (person.getOrderKey().compareTo(spouse.getOrderKey()) < 0)
                ? person.getId() + "-" + spouse.getId()
                : spouse.getId() + "-" + person.getId();
    }
}
