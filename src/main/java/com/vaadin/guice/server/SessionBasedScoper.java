/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.guice.server;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

class SessionBasedScoper implements Scope, SessionDestroyListener, SessionInitListener {

    private static final int MAP_INIT_SIZE_MAX = 384;
    private static final int MAP_POOL_MAX_SIZE = 12;
    private final Stack<Map<Key, Object>> mapPool = new Stack<Map<Key, Object>>();
    private final Map<VaadinSession, Map<Key, Object>> sessionToScopedObjectsMap = new ConcurrentHashMap<VaadinSession, Map<Key, Object>>();
    private final SessionProvider sessionProvider;
    private int mapSizeInit = 16;

    SessionBasedScoper(SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public T get() {
                Map<Key, Object> map = sessionToScopedObjectsMap.get(sessionProvider.getCurrentSession());

                T t = (T) map.get(key);

                if (t == null) {
                    t = unscoped.get();
                    map.put(key, t);
                }

                return t;
            }
        };
    }

    private Map<Key, Object> getMap() {
        synchronized (mapPool) {
            return mapPool.isEmpty()
                    ? new HashMap<Key, Object>(mapSizeInit)
                    : mapPool.pop();
        }
    }

    private void returnMap(Map<Key, Object> map) {
        if (mapSizeInit < map.size()) {
            //if the returned map's size is larger than mapSizeInit, increase it but don't exceed MAP_INIT_SIZE_MAX
            mapSizeInit = Math.min(map.size(), MAP_INIT_SIZE_MAX);
        }

        if (map.size() <= MAP_INIT_SIZE_MAX) {
            synchronized (mapPool) {
                if (mapPool.size() < MAP_POOL_MAX_SIZE) {
                    map.clear();
                    mapPool.add(map);
                }
            }
        }
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
        returnMap(sessionToScopedObjectsMap.remove(event.getSession()));
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        sessionToScopedObjectsMap.put(event.getSession(), getMap());
    }
}
