package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtils {

    public static Pair<Map<Integer, Integer>, Map<Integer, List<Integer>>> calculateShortestPathFromSource(
            EnrichedGedcom gedcom,
            EnrichedPerson source,
            boolean isSiblingDirectRel,
            boolean includePaths) {

        Map<Integer, Integer> distances = new HashMap<>(gedcom.getPeople().size());
        Map<Integer, List<Integer>> shortestPaths = new HashMap<>(gedcom.getPeople().size());

        distances.put(source.getId(), 0);
        if (includePaths) {
            shortestPaths.put(source.getId(), new ArrayList<>());
        }

        Queue<Pair<Integer, Integer>> unsettledNodes = new PriorityQueue<>(
                gedcom.getPeople().size(),
                Comparator.comparing(Pair::getRight));
        Set<Integer> settledNodes = new HashSet<>(gedcom.getPeople().size());

        unsettledNodes.add(Pair.of(source.getId(), 0));

        while (!unsettledNodes.isEmpty()) {
            Pair<Integer, Integer> currentData = unsettledNodes.remove();
            Integer currentNodeId = currentData.getLeft();

            // Unsettled nodes can contain repeated node ids, cause it adds many distances to it before processing it
            if (settledNodes.contains(currentNodeId)) {
                continue;
            }

            Integer sourceDistance = currentData.getRight();
            EnrichedPerson currentNode = Objects.requireNonNull(gedcom.getPersonById(currentNodeId));

            List<EnrichedPerson> directRelatives = Stream
                    .of(
                            currentNode.getParents(),
                            (isSiblingDirectRel ? currentNode.getAllSiblings() : List.<EnrichedPerson>of()),
                            currentNode.getSpouses(),
                            currentNode.getChildren())
                    .flatMap(List::stream)
                    .toList();

            for (EnrichedPerson adjacentNode : directRelatives) {
                if (!settledNodes.contains(adjacentNode.getId())) {
                    Integer adjacentDistance = calculateMinimumDistance(
                            adjacentNode.getId(),
                            1,
                            currentNodeId,
                            sourceDistance,
                            distances,
                            shortestPaths,
                            includePaths);
                    unsettledNodes.add(Pair.of(adjacentNode.getId(), adjacentDistance));
                }
            }

            settledNodes.add(currentNodeId);
            if (includePaths) {
                shortestPaths.get(currentNodeId).add(currentNodeId);
            }
        }

        return Pair.of(distances, shortestPaths);
    }

    private static Integer calculateMinimumDistance(
            Integer evaluationNodeId,
            @SuppressWarnings("SameParameterValue") Integer edgeWeigh,
            Integer sourceNodeId,
            Integer sourceDistance,
            Map<Integer, Integer> distances,
            Map<Integer, List<Integer>> shortestPaths,
            boolean includePaths) {

        Integer newDistance = sourceDistance + edgeWeigh;
        Integer evaluationNodeDistance = distances.getOrDefault(evaluationNodeId, Integer.MAX_VALUE);

        if (newDistance < evaluationNodeDistance) {
            distances.put(evaluationNodeId, newDistance);

            if (includePaths) {
                List<Integer> shortestPath = new ArrayList<>(shortestPaths.getOrDefault(sourceNodeId, List.of()));
                shortestPath.add(sourceNodeId);
                shortestPaths.put(evaluationNodeId, shortestPath);
            }

            return newDistance;
        }

        return evaluationNodeDistance;
    }

}
