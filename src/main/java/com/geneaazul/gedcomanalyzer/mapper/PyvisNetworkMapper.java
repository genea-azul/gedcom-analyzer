package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Surname;

import org.springframework.stereotype.Component;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PyvisNetworkMapper {

    private static final double ROOT_NODE_SIZE = 36;
    private static final double ROOT_NODE_BORDER_WIDTH = 4;
    private static final String ROOT_NODE_COLOR = "#f0f0f0";
    private static final String SHAPE_DISTINGUISHED_PERSON = "star";
    private static final String SHAPE_COUPLE_NODE = "dot";

    private final RelationshipMapper relationshipMapper;

    public String[] toPyvisNodeCsvRecord(
            EnrichedPerson person,
            boolean obfuscateLiving,
            Map<String, String[]> countryColorsMap,
            String defaultLabel,
            double borderWidth,
            String[] defaultColors,
            double size,
            boolean isRootPerson,
            FormattedRelationship relationshipToRoot) {
        double nodeSize = isRootPerson ? ROOT_NODE_SIZE : size;
        double nodeBorderWidth = isRootPerson ? ROOT_NODE_BORDER_WIDTH : borderWidth;
        String nodeColor = isRootPerson ? ROOT_NODE_COLOR : person.getPlaceOfBirth()
                .map(Place::country)
                .map(countryColorsMap::get)
                .filter(countryColors -> StringUtils.isNotEmpty(countryColors[0]))
                .map(countryColors -> person.isAlive()
                        ? countryColors[0]
                        : countryColors[1])
                .orElseGet(() -> person.isAlive()
                        ? defaultColors[0]
                        : defaultColors[1]);
        return new String[] {
                person.getId().toString(),
                getPyvisNodeLabel(person, defaultLabel),
                getPyvisNodeTitle(person, relationshipToRoot),
                person.isDistinguishedPerson()
                        ? SHAPE_DISTINGUISHED_PERSON
                        : null,
                String.valueOf(nodeBorderWidth),
                nodeColor,
                String.valueOf(nodeSize)
        };
    }

    private String getPyvisNodeTitle(EnrichedPerson person, FormattedRelationship relationshipToRoot) {
        String title = relationshipMapper
                .displayNameInSpanish(person.getDisplayName());
        if (person.getDateOfBirth().isPresent()) {
            title = title + " - " + person.getDateOfBirth().get().getYear();
        }
        if (person.getPlaceOfBirth().isPresent()) {
            title = title + " - " + person.getPlaceOfBirth().get().country();
        }
        if (StringUtils.isNotBlank(relationshipToRoot.relationshipDesc())) {
            String adoptive = relationshipToRoot.adoption() != null ? " (" + relationshipToRoot.adoptionBranch() + ")" : "";
            title = title + " — " + relationshipToRoot.relationshipDesc() + adoptive;
        }
        return title;
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

    public String[] toPyvisCoupleNodeCsvRecord(
            EnrichedPerson person,
            EnrichedPerson spouse,
            boolean noChildren,
            boolean obfuscateLiving,
            String defaultLabel,
            double borderWidth,
            String color,
            double size) {
        String coupleNodeId = buildCoupleNodeId(person, spouse, noChildren);
        return new String[] {
                coupleNodeId,
                " ",
                "Pareja: " + person.getDisplayName() + " - " + spouse.getDisplayName(),
                SHAPE_COUPLE_NODE,
                String.valueOf(borderWidth),
                color,
                String.valueOf(size)
        };
    }

    public String[] toPyvisSpouseEdgeCsvRecord(
            EnrichedPerson person,
            EnrichedPerson spouse,
            boolean noChildren,
            boolean separated,
            String defaultTitle,
            String separatedTitle,
            double weight,
            double width,
            String color,
            int dashes) {
        String coupleNodeId = buildCoupleNodeId(person, spouse, noChildren);
        return new String[] {
                person.getId().toString(),
                coupleNodeId,
                separated
                        ? separatedTitle
                        : (noChildren ? defaultTitle : null),
                String.valueOf(weight),
                String.valueOf(width),
                color,
                String.valueOf(dashes)
        };
    }

    public String[] toPyvisChildEdgeCsvRecord(
            String sourceId,
            String childId,
            boolean singleParent,
            boolean adopted,
            String defaultTitle,
            String adoptedTitle,
            int adoptedDashes,
            double weight,
            double width,
            String color,
            int dashes) {
        return new String[] {
                sourceId,
                childId,
                singleParent
                        ? defaultTitle
                        : (adopted ? adoptedTitle : null),
                String.valueOf(weight),
                String.valueOf(width),
                color,
                String.valueOf(adopted ? adoptedDashes : dashes)
        };
    }

    public String buildCoupleNodeId(EnrichedPerson person, EnrichedPerson spouse, boolean noChildren) {
        if (noChildren) {
            return spouse.getId().toString();
        }

        return (person.getOrderKey().compareTo(spouse.getOrderKey()) < 0)
                ? person.getId() + "-" + spouse.getId()
                : spouse.getId() + "-" + person.getId();
    }
}
