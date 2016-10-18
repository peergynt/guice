package com.google.common.eventbus;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Singleton
class GlobalEventBusImpl extends EventBus {

    private final Map<VaadinSession, Set<Object>> registeredObjectsBySession = new ConcurrentHashMap<VaadinSession, Set<Object>>();
    private final Map<Object, UI> registeredObjectsToUI;
    private final Logger logger = Logger.getLogger(getClass().getCanonicalName());
    private Provider<VaadinSession> vaadinSessionProvider;

    @Inject
    GlobalEventBusImpl(VaadinService vaadinService, Provider<VaadinSession> vaadinSessionProvider, final @Named("_registeredObjectsToUI") Map<Object, UI> _registeredObjectsToUI) {
        super("default", MoreExecutors.directExecutor(),
                new Dispatcher() {
                    @Override
                    void dispatch(final Object event, Iterator<Subscriber> subscribers) {
                        while (subscribers.hasNext()) {
                            final Subscriber subscriber = subscribers.next();

                            UI ui = _registeredObjectsToUI.get(subscriber.target);

                            if (ui != null) {
                                ui.access(new Runnable() {
                                    @Override
                                    public void run() {
                                        subscriber.dispatchEvent(event);
                                    }
                                });
                            } else {
                                //use current thread
                                subscriber.dispatchEvent(event);
                            }
                        }
                    }
                }, LoggingHandler.INSTANCE);

        this.vaadinSessionProvider = vaadinSessionProvider;

        this.registeredObjectsToUI = _registeredObjectsToUI;

        vaadinService.addSessionDestroyListener(new SessionDestroyListener() {
            @Override
            public void sessionDestroy(SessionDestroyEvent sessionDestroyEvent) {
                releaseAll(sessionDestroyEvent.getSession());
            }
        });
    }

    private void releaseAll(VaadinSession vaadinSession) {

        Set<Object> registeredObjects = registeredObjectsBySession.remove(vaadinSession);

        if (registeredObjects != null) {
            try {
                for (Object registeredObject : registeredObjects) {
                    registeredObjectsToUI.remove(registeredObject);
                }

                for (Object registeredObject : registeredObjects) {
                    super.unregister(registeredObject);
                }
            } finally {
                ObjectSetPool.returnMap(registeredObjects);
            }
        }
    }

    @Override
    public void register(Object object) {
        checkNotNull(object);

        if (object instanceof Component) {
            UI ui = ((Component) object).getUI();

            if (ui != null) {
                final UI formerEntryUI = registeredObjectsToUI.put(object, ui);

                if (formerEntryUI != null) {
                    checkState(formerEntryUI == ui);
                }
            }
        }

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

        try {
            registeredObjectsBySession.get(vaadinSessionProvider.get()).remove(object);
        } catch (RuntimeException e) {
            //should never happen
            logger.severe(e.getMessage());
        }

        try {
            registeredObjectsToUI.remove(object);
        } catch (RuntimeException e) {
            //should never happen
            logger.severe(e.getMessage());
        }

        super.unregister(object);
    }
}
