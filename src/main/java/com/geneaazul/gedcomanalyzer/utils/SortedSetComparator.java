package com.geneaazul.gedcomanalyzer.utils;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

import jakarta.annotation.Nullable;

public class SortedSetComparator <E extends Comparable<E>> implements Comparator<SortedSet<E>> {

    @Override
    public int compare(@Nullable SortedSet<E> set1, @Nullable SortedSet<E> set2) {
        if (set1 == set2) {
            return 0;
        }
        if (set1 == null) {
            return -1;
        }
        if (set2 == null) {
            return 1;
        }
        if (set1.size() != set2.size()) {
            return set1.size() < set2.size() ? -1 : 1;
        }

        Iterator<E> iterator2 = set2.iterator();
        for (E element1 : set1) {
            int comparison = element1.compareTo(iterator2.next());
            if (comparison != 0) {
                return comparison;
            }
        }

        return 0;
    }

}
