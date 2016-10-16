package com.vaadin.guice.bus;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import com.vaadin.guice.bus.events.GlobalEvent;
import com.vaadin.guice.bus.events.SessionEvent;
import com.vaadin.guice.bus.events.UIEvent;
import com.vaadin.guice.bus.events.ViewEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

class SubscriptionTypeListener implements TypeListener {

    private static final int GLOBAL = 1;
    private static final int SESSION = 2;
    private static final int UI = 4;
    private static final int VIEW = 8;

    private final Provider<Injector> injectorProvider;
    private final Map<Class<?>, Integer> classToScopesMap = new ConcurrentHashMap<Class<?>, Integer>();

    SubscriptionTypeListener(Provider<Injector> injectorProvider) {
        this.injectorProvider = injectorProvider;
    }

    @Override
    public <I> void hear(final TypeLiteral<I> type, TypeEncounter<I> encounter) {
        encounter.register(new InjectionListener<I>() {
            @Override
            public void afterInjection(I injectee) {
                int scopes = getScopes(injectee.getClass());

                if ((scopes & GLOBAL) == GLOBAL) {
                    injectorProvider.get().getInstance(GlobalEventBus.class).register(injectee);
                }

                if ((scopes & SESSION) == SESSION) {
                    injectorProvider.get().getInstance(SessionEventBus.class).register(injectee);
                }

                if ((scopes & UI) == UI) {
                    injectorProvider.get().getInstance(UIEventBus.class).register(injectee);
                }

                if ((scopes & VIEW) == VIEW) {
                    injectorProvider.get().getInstance(ViewEventBus.class).register(injectee);
                }
            }

            private int getScopes(Class<?> clazz) {

                Integer scopesFromCache = classToScopesMap.get(clazz);

                if (scopesFromCache != null) {
                    return scopesFromCache;
                }

                int scopes = 0;

                boolean globalRegistered = false;
                boolean sessionRegistered = false;
                boolean uiRegistered = false;
                boolean viewRegistered = false;

                for (Method method : clazz.getMethods()) {

                    if ((method.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
                        continue;
                    }

                    if (method.getAnnotation(Subscribe.class) == null) {
                        continue;
                    }

                    checkArgument(method.getParameterTypes().length == 1,
                            "method %s is annotated with @subscribe but does not have exactly one parameter",
                            method
                    );

                    Class<?> parameter = method.getParameterTypes()[0];

                    if (!globalRegistered && GlobalEvent.class.isAssignableFrom(parameter)) {
                        scopes = scopes | GLOBAL;
                        globalRegistered = true;
                    }

                    if (!sessionRegistered && SessionEvent.class.isAssignableFrom(parameter)) {
                        scopes = scopes | SESSION;
                        sessionRegistered = true;
                    }

                    if (!uiRegistered && UIEvent.class.isAssignableFrom(parameter)) {
                        scopes = scopes | UI;
                        uiRegistered = true;
                    }

                    if (!viewRegistered && ViewEvent.class.isAssignableFrom(parameter)) {
                        scopes = scopes | VIEW;
                        viewRegistered = true;
                    }
                }

                classToScopesMap.put(clazz, scopes);

                return scopes;
            }
        });
    }
}
