package com.vaadin.guice.server;

import com.google.inject.Key;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

final class KeyObjectMapPool {
    private static final int KEY_OBJECT_MAP_REUSE_SIZE_MAX = 1024;
    private static final Deque<Map<Key<?>, Object>> pool = new ArrayDeque<Map<Key<?>, Object>>();

    private KeyObjectMapPool() {
    }

    static Map<Key<?>, Object> getKeyObjectMap() {
        synchronized (pool) {
            return pool.isEmpty()
                    ? new HashMap<Key<?>, Object>()
                    : pool.pop();
        }
    }

    static void returnKeyObjectMap(Map<Key<?>, Object> objectSet) {
        if (objectSet.size() <= KEY_OBJECT_MAP_REUSE_SIZE_MAX) {
            synchronized (pool) {
                objectSet.clear();
                pool.add(objectSet);
            }
        }
    }
}
