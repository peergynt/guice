package com.vaadin.guice.server;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

/**
 * This interface is to be applied to {@link Module} classes that are
 * loaded by a {@link GuiceVaadinServlet} and need an {@link Injector}
 * instance
 */
public interface NeedsInjector extends Module {
    /**
     * this method will be called by guice-vaadin before 'configure' is called,
     * @param injectorProvider the {@link Provider} for the {@link Injector}
     */
    void setInjectorProvider(Provider<Injector> injectorProvider);
}
