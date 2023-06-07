package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

public class GraphService {

    static final int INF = 999999;

    // A utility function to find the vertex with minimum
    // distance value, from the set of vertices not yet
    // included in shortest path tree
    static int minDistance(int[] dist, boolean[] sptSet) {
        final int V = dist.length;
        // Initialize min value
        int min = INF, min_index = -1;

        for (int v = 0; v < V; v++)
            if (!sptSet[v] && dist[v] <= min) {
                min = dist[v];
                min_index = v;
            }
        return min_index;
    }

    // A utility function to print the constructed distance
    // array
    static void printSolution(Map<String, Map<String, Integer>> dist) {

        System.out.println(
                "Following matrix shows the shortest distances "
                        + "between every pair of vertices:");
        List<String> ids = List.copyOf(dist.keySet());
        for (String i : ids) {
            for (String j : ids) {
                int distIJ = dist.getOrDefault(i, Map.of()).getOrDefault(j, INF);
                if (distIJ == INF)
                    System.out.printf("%7s", "INF");
                else
                    System.out.printf("%7d", distIJ);
            }
            System.out.println();
        }
    }

    public static Map<String, Map<String, Integer>> floydWarshall(List<EnrichedPerson> people) {

        Map<String, Map<String, Integer>> graph = people
                .stream()
                .map(person -> Map.entry(
                        person.getId(),
                        Stream
                                .of(
                                        person.getParents(),
                                        person.getSpouses(),
                                        person.getChildren())
                                .flatMap(List::stream)
                                .map(EnrichedPerson::getId)
                                .peek(ss -> System.out.println(person.getId() + " -> " + ss))
                                .collect(Collectors.toMap(Function.identity(), id -> 1))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return floydWarshall(graph);
    }

    // Solves the all-pairs shortest path problem using Floyd Warshall algorithm
    public static Map<String, Map<String, Integer>> floydWarshall(Map<String, Map<String, Integer>> graph) {

        Map<String, Map<String, Integer>> dist = graph
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new HashMap<>(e.getValue())));

        dist
                .forEach((key, value) -> value.put(key, 0));

        /* Add all vertices one by one to the set of
          intermediate vertices.
          ---> Before start of a iteration, we have shortest
          distances between all pairs of vertices such that
          the shortest distances consider only the vertices
          in set {0, 1, 2, .. k-1} as intermediate vertices.
          ----> After the end of a iteration, vertex no. k
          is added to the set of intermediate vertices and
          the set becomes {0, 1, 2, .. k} */

        List<String> ids = List.copyOf(graph.keySet());

        for (String k : ids) {
            // Pick all vertices as source one by one
            for (String i : ids) {
                // Pick all vertices as destination for the
                // above picked source
                for (String j : ids) {
                    // If vertex k is on the shortest path
                    // from i to j, then update the value of
                    // dist[i][j]

                    @Nullable Integer distIK = dist.get(i).get(k);
                    @Nullable Integer distKJ = dist.get(k).get(j);

                    if (distIK == null || distKJ == null) {
                        continue;
                    }

                    @Nullable Integer distIJ = dist.get(i).get(j);
                    Integer distIkJ = distIK + distKJ;

                    if (distIJ == null || distIkJ < distIJ) {
                        dist.get(i).put(j, distIkJ);
                    }
                }
            }
        }

        // Print the shortest distance matrix
        return dist;
    }
}
