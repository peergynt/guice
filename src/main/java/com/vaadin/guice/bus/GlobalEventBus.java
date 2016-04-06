package com.vaadin.guice.bus;

import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;

import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Singleton
@SuppressWarnings("unused")
public class GlobalEventBus extends EventBus {

    private final Map<VaadinSession, Set<Object>> registeredObjectsBySession = new HashMap<VaadinSession, Set<Object>>();

    GlobalEventBus() {
        VaadinService.getCurrent().addSessionInitListener(new SessionInitListener() {
            @Override
            public void sessionInit(SessionInitEvent sessionInitEvent) throws ServiceException {
                registeredObjectsBySession.put(sessionInitEvent.getSession(), new HashSet<Object>());
            }
        });

        VaadinService.getCurrent().addSessionDestroyListener(new SessionDestroyListener() {
            @Override
            public void sessionDestroy(SessionDestroyEvent sessionDestroyEvent) {
                releaseAll(sessionDestroyEvent.getSession());
            }
        });
    }

    private void releaseAll(VaadinSession vaadinSession){
        Set<Object> registeredObjects = registeredObjectsBySession.remove(vaadinSession);

        checkState(registeredObjects != null);

        for (Object registeredObject : registeredObjects) {
            super.unregister(registeredObject);
        }
    }

    @Override
    public void register(Object object) {
        checkNotNull(object);
        registeredObjectsBySession.get(VaadinSession.getCurrent()).add(object);
        super.register(object);
    }

    @Override
    public void unregister(Object object) {
        checkNotNull(object);
        registeredObjectsBySession.get(VaadinSession.getCurrent()).remove(object);
        super.unregister(object);
    }
}

