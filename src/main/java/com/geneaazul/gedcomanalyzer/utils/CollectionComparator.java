package com.geneaazul.gedcomanalyzer.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CollectionComparator<E extends Comparable<E>> implements Comparator<Collection<E>> {

    private final boolean biggerCollectionPriority;

    public CollectionComparator() {
        this(false);
    }

    @Override
    public int compare(Collection<E> c1, Collection<E> c2) {
        if (c1 == c2) {
            return 0;
        }
        if (c1.size() != c2.size()) {
            return biggerCollectionPriority
                    ? (c1.size() > c2.size() ? -1 : 1)
                    : (c1.size() < c2.size() ? -1 : 1);
        }

        Iterator<E> iterator2 = c2.iterator();
        for (E element1 : c1) {
            int comparison = element1.compareTo(iterator2.next());
            if (comparison != 0) {
                return comparison;
            }
        }

        return 0;
    }

}
