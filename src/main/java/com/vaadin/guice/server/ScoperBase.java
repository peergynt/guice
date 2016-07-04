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
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkState;

abstract class ScoperBase<SCOPE_BASE> implements Scope, SessionDestroyListener, SessionInitListener {
    private final Provider<VaadinSession> vaadinSessionProvider;
    private final Provider<SCOPE_BASE> currentInstanceProvider;
    private final Map<VaadinSession, Map<SCOPE_BASE, Map<Key<?>, Object>>> sessionToScopedObjectsMap = new ConcurrentHashMap<VaadinSession, Map<SCOPE_BASE, Map<Key<?>, Object>>>();
    private Map<Key<?>, Object> currentInitializationScopeSet = null;

    ScoperBase(Provider<SCOPE_BASE> currentInstanceProvider, Provider<VaadinSession> vaadinSessionProvider) {
        this.currentInstanceProvider = currentInstanceProvider;
        this.vaadinSessionProvider = vaadinSessionProvider;
    }

    void startInitialization() {
        checkState(currentInitializationScopeSet == null);
        currentInitializationScopeSet = KeyObjectMapPool.leaseMap();
    }

    void rollbackInitialization() {
        checkState(currentInitializationScopeSet != null);
        KeyObjectMapPool.returnMap(currentInitializationScopeSet);
        currentInitializationScopeSet = null;
    }

    void endInitialization(SCOPE_BASE scopeBase) {
        checkState(currentInitializationScopeSet != null);
        final Map<SCOPE_BASE, Map<Key<?>, Object>> scopedObjectsPerInstance = sessionToScopedObjectsMap.get(vaadinSessionProvider.get());
        checkState(scopedObjectsPerInstance != null);
        scopedObjectsPerInstance.put(scopeBase, currentInitializationScopeSet);
        currentInitializationScopeSet = null;
    }

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public T get() {
                Map<Key<?>, Object> scopedObjects = getCurrentScopeMap();

                T t = (T) scopedObjects.get(key);

                if (t == null) {
                    t = unscoped.get();
                    scopedObjects.put(key, t);
                }

                return t;
            }
        };
    }

    private Map<Key<?>, Object> getCurrentScopeMap() {
        Map<Key<?>, Object> scopedObjects;

        if (currentInitializationScopeSet != null) {
            scopedObjects = currentInitializationScopeSet;
        } else {
            final Map<SCOPE_BASE, Map<Key<?>, Object>> scopedObjectsByInstance = sessionToScopedObjectsMap.get(vaadinSessionProvider.get());
            checkState(scopedObjectsByInstance != null);
            scopedObjects = scopedObjectsByInstance.get(currentInstanceProvider.get());
            checkState(scopedObjects != null);
        }
        return scopedObjects;
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
        final Map<SCOPE_BASE, Map<Key<?>, Object>> map = sessionToScopedObjectsMap.remove(event.getSession());

        for (Map<Key<?>, Object> keyObjectMap : map.values()) {
            KeyObjectMapPool.returnMap(keyObjectMap);
        }
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        sessionToScopedObjectsMap.put(event.getSession(), new HashMap<SCOPE_BASE, Map<Key<?>, Object>>());
    }
}
