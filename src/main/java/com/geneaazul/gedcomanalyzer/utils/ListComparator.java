package com.geneaazul.gedcomanalyzer.utils;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ListComparator<E extends Comparable<E>> implements Comparator<List<E>> {

    @Override
    public int compare(List<E> list1, List<E> list2) {
        if (list1 == list2) {
            return 0;
        }
        if (list1.size() != list2.size()) {
            return list1.size() < list2.size() ? -1 : 1;
        }

        Iterator<E> iterator2 = list2.iterator();
        for (E element1 : list1) {
            int comparison = element1.compareTo(iterator2.next());
            if (comparison != 0) {
                return comparison;
            }
        }

        return 0;
    }

}
