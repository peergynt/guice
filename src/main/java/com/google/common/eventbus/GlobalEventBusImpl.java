package com.google.common.eventbus;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@SuppressWarnings("unused")
class GlobalEventBusImpl extends EventBus {

    private static final Dispatcher DISPATCHER = new Dispatcher() {
        @Override
        void dispatch(final Object event, Iterator<Subscriber> subscribers) {
            while (subscribers.hasNext()) {
                final Subscriber subscriber = subscribers.next();

                if (subscriber.target instanceof Component) {
                    ((Component) subscriber.target).getUI().access(new Runnable() {
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
    };
    private final Map<VaadinSession, Set<Object>> registeredObjectsBySession = new ConcurrentHashMap<VaadinSession, Set<Object>>();
    private final Logger logger = Logger.getLogger(getClass().getCanonicalName());
    private Provider<VaadinSession> vaadinSessionProvider;

    @Inject
    GlobalEventBusImpl(VaadinService vaadinService, Provider<VaadinSession> vaadinSessionProvider) {
        super("default", MoreExecutors.directExecutor(), DISPATCHER, LoggingHandler.INSTANCE);

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

        if (registeredObjects != null) {
            try {
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
        } finally {
            super.unregister(object);
        }
    }
}
