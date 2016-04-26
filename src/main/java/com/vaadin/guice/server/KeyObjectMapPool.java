package com.vaadin.guice.server;

import com.google.inject.Key;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

final class KeyObjectMapPool {
    private KeyObjectMapPool(){
    }

    private static final int KEY_OBJECT_MAP_INIT_SIZE_MAX = 1024;
    private static final int KEY_OBJECT_MAP_POOL_MAX_SIZE = Runtime.getRuntime().availableProcessors() - 1;
    private static final Deque<Map<Key, Object>> pool = new ArrayDeque<Map<Key, Object>>();
    private static int setSizeInit = 16;
    
    public static Map<Key, Object> getKeyObjectMap() {
        synchronized (pool) {
            return pool.isEmpty()
                    ? new HashMap<Key, Object>(setSizeInit)
                    : pool.pop();
        }
    }

    public static void returnKeyObjectMap(Map<Key, Object> objectSet) {
        if (setSizeInit < objectSet.size()) {
            //if the returned objectSet's size is larger than setSizeInit, increase it but don't exceed KEY_OBJECT_MAP_INIT_SIZE_MAX
            setSizeInit = Math.min(objectSet.size(), KEY_OBJECT_MAP_INIT_SIZE_MAX);
        }

        if (objectSet.size() <= KEY_OBJECT_MAP_INIT_SIZE_MAX) {
            synchronized (pool) {
                if (pool.size() < KEY_OBJECT_MAP_POOL_MAX_SIZE) {
                    objectSet.clear();
                    pool.add(objectSet);
                }
            }
        }
    }
}    

