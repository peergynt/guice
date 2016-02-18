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

import com.google.common.base.Optional;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkState;

class UIScoper implements Scope, SessionDestroyListener, SessionInitListener {

    private final Map<VaadinSession, Map<UI, Map<Key, Object>>> sessionToScopedObjectsMap = new ConcurrentHashMap<VaadinSession, Map<UI, Map<Key, Object>>>();
    private final SessionProvider sessionProvider;
    private final CurrentUIProvider currentUIProvider;
    private Optional<Map<Key, Object>> currentInitializationScopeSet = Optional.absent();

    UIScoper(SessionProvider sessionProvider, CurrentUIProvider currentUIProvider) {
        this.sessionProvider = sessionProvider;
        this.currentUIProvider = currentUIProvider;
    }

    public void startInitialization() {
        checkState(!currentInitializationScopeSet.isPresent());
        currentInitializationScopeSet = Optional.of((Map<Key, Object>) new HashMap<Key, Object>());
    }

    public void rollbackInitialization() {
        checkState(currentInitializationScopeSet.isPresent());
        currentInitializationScopeSet = Optional.absent();
    }

    public void endInitialization(UI ui) {
        checkState(currentInitializationScopeSet.isPresent());
        final Map<UI, Map<Key, Object>> uiScopes = sessionToScopedObjectsMap.get(sessionProvider.getCurrentSession());
        checkState(uiScopes != null);
        uiScopes.put(ui, currentInitializationScopeSet.get());
        currentInitializationScopeSet = Optional.absent();
    }

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public T get() {
                Map<Key, Object> scopedObjects = getCurrentScopeMap();

                T t = (T) scopedObjects.get(key);

                if (t == null) {
                    t = unscoped.get();
                    scopedObjects.put(key, t);
                }

                return t;
            }
        };
    }

    Map<Key, Object> getCurrentScopeMap() {
        Map<Key, Object> scopedObjects;

        if (currentInitializationScopeSet.isPresent()) {
            scopedObjects = currentInitializationScopeSet.get();
        } else {
            final Map<UI, Map<Key, Object>> sessionToUIScopes = sessionToScopedObjectsMap.get(sessionProvider.getCurrentSession());
            checkState(sessionToUIScopes != null);
            scopedObjects = sessionToUIScopes.get(currentUIProvider.getCurrentUI());
            checkState(scopedObjects != null);
        }
        return scopedObjects;
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
        checkState(sessionToScopedObjectsMap.remove(event.getSession()) != null);
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        sessionToScopedObjectsMap.put(event.getSession(), new HashMap<UI, Map<Key, Object>>());
    }
}
