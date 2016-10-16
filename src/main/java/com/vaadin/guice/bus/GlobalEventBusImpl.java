package com.vaadin.guice.bus;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
final class GlobalEventBusImpl extends EventBus implements GlobalEventBus {

    private final Map<VaadinSession, Set<Object>> registeredObjectsBySession = new ConcurrentHashMap<VaadinSession, Set<Object>>();
    private final Provider<VaadinSession> vaadinSessionProvider;

    @Inject
    GlobalEventBusImpl(VaadinService vaadinService, Provider<VaadinSession> vaadinSessionProvider) {
        this.vaadinSessionProvider = vaadinSessionProvider;

        vaadinService.addSessionDestroyListener(new SessionDestroyListener() {
            @Override
            public void sessionDestroy(SessionDestroyEvent sessionDestroyEvent) {
                releaseAll(sessionDestroyEvent.getSession());
            }
        });
    }

    private void releaseAll(VaadinSession vaadinSession) {
        Set<Object> registeredObjects = registeredObjectsBySession.remove(vaadinSession);

        if (registeredObjects == null) {
            return;
        }

        try {
            for (Object registeredObject : registeredObjects) {
                super.unregister(registeredObject);
            }
        } finally {
            ObjectSetPool.returnMap(registeredObjects);
        }
    }

    @Override
    public void register(Object object) {
        checkNotNull(object);

        final Set<Object> registeredObjects = getRegisteredObjects();

        registeredObjects.add(object);
        super.register(object);
    }

    private Set<Object> getRegisteredObjects() {
        Set<Object> registeredObjects = registeredObjectsBySession.get(vaadinSessionProvider.get());

        if (registeredObjects == null) {
            registeredObjects = ObjectSetPool.leaseMap();
            registeredObjectsBySession.put(vaadinSessionProvider.get(), registeredObjects);
        }

        return registeredObjects;
    }

    @Override
    public void unregister(Object object) {
        checkNotNull(object);
        registeredObjectsBySession.get(vaadinSessionProvider.get()).remove(object);
        super.unregister(object);
    }
}

