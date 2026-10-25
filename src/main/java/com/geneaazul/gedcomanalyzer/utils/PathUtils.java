package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import jakarta.annotation.Nullable;

import lombok.experimental.UtilityClass;

/**
 * Shortest paths over direct family ties (parents, optionally siblings, spouses, children).
 *
 * <p>Every tie counts as one step, so a breadth-first search finds shortest paths in O(people)
 * time and memory. Ties between equally short paths are resolved deterministically in the order
 * parents, siblings, spouses, children.
 */
@UtilityClass
public class PathUtils {

    public record ShortestPaths(int sourceId, Map<Integer, Integer> distances, Map<Integer, Integer> predecessors) {

        /**
         * @return the distance from the source, or {@code null} if the person is not reachable
         */
        @Nullable
        public Integer distanceTo(int personId) {
            return distances.get(personId);
        }

        /**
         * @return person ids from the source to the target (both included), or an empty list if unreachable
         */
        public List<Integer> pathTo(int personId) {
            if (!distances.containsKey(personId)) {
                return List.of();
            }
            List<Integer> path = new ArrayList<>(distances.get(personId) + 1);
            for (Integer id = personId; id != null; id = predecessors.get(id)) {
                path.add(id);
            }
            Collections.reverse(path);
            return path;
        }
    }

    public static ShortestPaths calculateShortestPathsFromSource(
            EnrichedGedcom gedcom,
            EnrichedPerson source,
            boolean isSiblingDirectRel) {
        return breadthFirstSearch(gedcom, source, isSiblingDirectRel, null);
    }

    /**
     * Stops as soon as the target is reached instead of exploring the whole tree.
     */
    public static List<Integer> calculateShortestPath(
            EnrichedGedcom gedcom,
            EnrichedPerson source,
            EnrichedPerson target,
            boolean isSiblingDirectRel) {
        return breadthFirstSearch(gedcom, source, isSiblingDirectRel, target.getId()).pathTo(target.getId());
    }

    private static ShortestPaths breadthFirstSearch(
            EnrichedGedcom gedcom,
            EnrichedPerson source,
            boolean isSiblingDirectRel,
            @Nullable Integer targetId) {

        Map<Integer, Integer> distances = new HashMap<>();
        Map<Integer, Integer> predecessors = new HashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();

        distances.put(source.getId(), 0);
        queue.add(source.getId());

        while (!queue.isEmpty()) {
            Integer currentId = queue.remove();
            if (currentId.equals(targetId)) {
                break;
            }
            EnrichedPerson current = Objects.requireNonNull(gedcom.getPersonById(currentId));
            int nextDistance = distances.get(currentId) + 1;

            visit(current.getParents(), currentId, nextDistance, distances, predecessors, queue);
            if (isSiblingDirectRel) {
                visit(current.getAllSiblings(), currentId, nextDistance, distances, predecessors, queue);
            }
            visit(current.getSpouses(), currentId, nextDistance, distances, predecessors, queue);
            visit(current.getChildren(), currentId, nextDistance, distances, predecessors, queue);
        }

        return new ShortestPaths(source.getId(), distances, predecessors);
    }

    private static void visit(
            List<EnrichedPerson> relatives,
            Integer fromId,
            int distance,
            Map<Integer, Integer> distances,
            Map<Integer, Integer> predecessors,
            Queue<Integer> queue) {
        for (EnrichedPerson relative : relatives) {
            if (distances.putIfAbsent(relative.getId(), distance) == null) {
                predecessors.put(relative.getId(), fromId);
                queue.add(relative.getId());
            }
        }
    }

}
