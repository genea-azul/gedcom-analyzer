package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PathUtilsTest {

    /*
     *   1 Father ── 2 Mother        7 Unrelated
     *          │
     *   ┌──────┴──────┐
     *   3 Son ── 4 Wife   6 Daughter
     *       │
     *   5 Grandson
     */
    private final Map<Integer, EnrichedPerson> people = new HashMap<>();
    private EnrichedGedcom gedcom;

    private EnrichedPerson person(int id) {
        return people.computeIfAbsent(id, key -> {
            EnrichedPerson person = mock(EnrichedPerson.class);
            when(person.getId()).thenReturn(key);
            when(person.getParents()).thenReturn(List.of());
            when(person.getAllSiblings()).thenReturn(List.of());
            when(person.getSpouses()).thenReturn(List.of());
            when(person.getChildren()).thenReturn(List.of());
            return person;
        });
    }

    @BeforeEach
    void setUp() {
        EnrichedPerson father = person(1), mother = person(2), son = person(3), wife = person(4), grandson = person(5), daughter = person(6);
        person(7);
        when(father.getSpouses()).thenReturn(List.of(mother));
        when(mother.getSpouses()).thenReturn(List.of(father));
        when(father.getChildren()).thenReturn(List.of(son, daughter));
        when(mother.getChildren()).thenReturn(List.of(son, daughter));
        when(son.getParents()).thenReturn(List.of(father, mother));
        when(daughter.getParents()).thenReturn(List.of(father, mother));
        when(son.getAllSiblings()).thenReturn(List.of(daughter));
        when(daughter.getAllSiblings()).thenReturn(List.of(son));
        when(son.getSpouses()).thenReturn(List.of(wife));
        when(wife.getSpouses()).thenReturn(List.of(son));
        when(son.getChildren()).thenReturn(List.of(grandson));
        when(wife.getChildren()).thenReturn(List.of(grandson));
        when(grandson.getParents()).thenReturn(List.of(son, wife));

        gedcom = mock(EnrichedGedcom.class);
        when(gedcom.getPersonById(org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(invocation -> people.get(invocation.<Integer>getArgument(0)));
    }

    @Test
    void distancesAndPathsFromTheSource() {
        PathUtils.ShortestPaths paths = PathUtils.calculateShortestPathsFromSource(gedcom, person(5), true);

        assertThat(paths.distanceTo(5)).isZero();
        assertThat(paths.distanceTo(1)).isEqualTo(2);
        assertThat(paths.distanceTo(6)).isEqualTo(2);
        assertThat(paths.pathTo(5)).containsExactly(5);
        assertThat(paths.pathTo(1)).containsExactly(5, 3, 1);
        assertThat(paths.pathTo(4)).containsExactly(5, 4);
    }

    @Test
    void siblingsCanBeDirectTiesOrNot() {
        assertThat(PathUtils.calculateShortestPathsFromSource(gedcom, person(3), true).pathTo(6)).containsExactly(3, 6);
        assertThat(PathUtils.calculateShortestPathsFromSource(gedcom, person(3), false).pathTo(6)).containsExactly(3, 1, 6);
    }

    @Test
    void equallyShortPathsPreferParents() {
        // 6 -> 3 via father or via mother: both length 2, father is listed first
        assertThat(PathUtils.calculateShortestPathsFromSource(gedcom, person(6), false).pathTo(3)).containsExactly(6, 1, 3);
    }

    @Test
    void unreachablePeopleHaveNoDistanceOrPath() {
        PathUtils.ShortestPaths paths = PathUtils.calculateShortestPathsFromSource(gedcom, person(5), true);

        assertThat(paths.distanceTo(7)).isNull();
        assertThat(paths.pathTo(7)).isEmpty();
        assertThat(PathUtils.calculateShortestPath(gedcom, person(5), person(7), true)).isEmpty();
    }

    @Test
    void singleTargetSearchMatchesTheFullSearch() {
        for (int target = 1; target <= 6; target++) {
            assertThat(PathUtils.calculateShortestPath(gedcom, person(5), person(target), true))
                    .isEqualTo(PathUtils.calculateShortestPathsFromSource(gedcom, person(5), true).pathTo(target));
        }
    }

}
