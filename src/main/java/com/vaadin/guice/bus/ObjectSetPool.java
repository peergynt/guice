package com.vaadin.guice.bus;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

final class ObjectSetPool {
    private static final int OBJECT_SET_REUSE_SIZE_MAX = 1024;
    private static final Deque<Set<Object>> pool = new ArrayDeque<Set<Object>>();

    private ObjectSetPool() {
    }

    static Set<Object> leaseMap() {
        synchronized (pool) {
            return pool.isEmpty()
                    ? new HashSet<Object>()
                    : pool.pop();
        }
    }

    static void returnMap(Set<Object> objectSet) {
        if (objectSet.size() <= OBJECT_SET_REUSE_SIZE_MAX) {
            synchronized (pool) {
                objectSet.clear();
                pool.add(objectSet);
            }
        }
    }
}
