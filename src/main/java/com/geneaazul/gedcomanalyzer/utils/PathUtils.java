package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtils {

    public static Pair<Map<Integer, Integer>, Map<Integer, List<Integer>>> calculateShortestPathFromSource(
            EnrichedGedcom gedcom,
            EnrichedPerson source) {

        Map<Integer, Integer> distances = new HashMap<>();
        Map<Integer, List<Integer>> shortestPaths = new HashMap<>();

        distances.put(source.getId(), 0);
        shortestPaths.put(source.getId(), List.of());

        Set<Integer> settledNodes = new HashSet<>();
        Set<Integer> unsettledNodes = new LinkedHashSet<>();

        unsettledNodes.add(source.getId());

        while (!unsettledNodes.isEmpty()) {
            Integer currentNodeId = getLowestDistanceNodeId(unsettledNodes, distances);
            EnrichedPerson currentNode = Objects.requireNonNull(gedcom.getPersonById(currentNodeId));
            unsettledNodes.remove(currentNodeId);

            List<EnrichedPerson> directRelatives = Stream
                    .of(
                            currentNode.getParents(),
                            currentNode.getSpouses(),
                            currentNode.getChildren())
                    .flatMap(List::stream)
                    .toList();

            Integer sourceDistance = distances.getOrDefault(currentNode.getId(), Integer.MAX_VALUE);

            for (EnrichedPerson adjacentNode : directRelatives) {
                if (!settledNodes.contains(adjacentNode.getId())) {
                    calculateMinimumDistance(adjacentNode, 1, currentNode.getId(), sourceDistance, distances, shortestPaths);
                    unsettledNodes.add(adjacentNode.getId());
                }
            }

            settledNodes.add(currentNode.getId());
            List<Integer> shortestPath = new ArrayList<>(shortestPaths.getOrDefault(currentNode.getId(), List.of()));
            shortestPath.add(currentNode.getId());
            shortestPaths.put(currentNode.getId(), List.copyOf(shortestPath));
        }

        return Pair.of(distances, shortestPaths);
    }

    private static Integer getLowestDistanceNodeId(Set<Integer> unsettledNodes, Map<Integer, Integer> distances) {
        Integer lowestDistanceNode = null;
        int lowestDistance = Integer.MAX_VALUE;
        for (Integer node : unsettledNodes) {
            int nodeDistance = distances.getOrDefault(node, Integer.MAX_VALUE);
            if (nodeDistance < lowestDistance) {
                lowestDistance = nodeDistance;
                lowestDistanceNode = node;
            }
        }
        return lowestDistanceNode;
    }

    private static void calculateMinimumDistance(
            EnrichedPerson evaluationNode,
            @SuppressWarnings("SameParameterValue") Integer edgeWeigh,
            Integer sourceNodeId,
            Integer sourceDistance,
            Map<Integer, Integer> distances,
            Map<Integer, List<Integer>> shortestPaths) {

        Integer evaluationNodeDistance = distances.getOrDefault(evaluationNode.getId(), Integer.MAX_VALUE);

        if (sourceDistance + edgeWeigh < evaluationNodeDistance) {
            distances.put(evaluationNode.getId(), sourceDistance + edgeWeigh);

            List<Integer> shortestPath = new ArrayList<>(shortestPaths.getOrDefault(sourceNodeId, List.of()));
            shortestPath.add(sourceNodeId);
            shortestPaths.put(evaluationNode.getId(), List.copyOf(shortestPath));
        }
    }
}
