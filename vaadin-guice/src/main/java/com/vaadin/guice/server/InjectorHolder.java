package com.vaadin.guice.server;

import com.google.inject.Injector;

final class InjectorHolder {

    private static Injector injector;

    public static Injector getInjector() {
        return injector;
    }

    public static void setInjector(Injector injector) {
        InjectorHolder.injector = injector;
    }
}
