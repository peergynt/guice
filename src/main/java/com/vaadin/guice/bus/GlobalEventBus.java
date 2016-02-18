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

@Singleton
public class GlobalEventBus extends EventBus implements SessionDestroyListener, SessionInitListener {

    private final Map<VaadinSession, Set<Object>> registeredObjectsBySession = new HashMap<VaadinSession, Set<Object>>();

    GlobalEventBus() {
        VaadinService.getCurrent().addSessionInitListener(this);
        VaadinService.getCurrent().addSessionDestroyListener(this);
    }

    @Override
    public void register(Object object) {
        registeredObjectsBySession.get(VaadinSession.getCurrent()).add(object);
        super.register(object);
    }

    @Override
    public void unregister(Object object) {
        registeredObjectsBySession.get(VaadinSession.getCurrent()).remove(object);
        super.unregister(object);
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
        for (Object registeredObject : registeredObjectsBySession.remove(event.getSession())) {
            super.unregister(registeredObject);
        }
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        registeredObjectsBySession.put(event.getSession(), new HashSet<Object>());
    }
}

