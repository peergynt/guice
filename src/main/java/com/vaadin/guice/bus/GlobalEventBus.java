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

/**
 * This class serves as a means to allow application-scope communication between objects.
 * GlobalEventBus is intended for events that are of 'global' interest, like updates to data that is
 * used by multiple UIs simultaneously. It is singleton-scoped and will release any subscribers once
 * their {@link VaadinSession} is ended in order to prevent memory leaks.
 *
 * <code> {@literal @}Inject private GlobalEventBus globalEventBus;
 *
 * ... globalEventBus.post(new DataSetOfGlobalInterestChangedEvent()); ...
 *
 * </code> </pre>
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
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

    private void releaseAll(VaadinSession vaadinSession) {
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

