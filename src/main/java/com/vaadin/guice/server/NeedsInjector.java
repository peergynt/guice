package com.vaadin.guice.server;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

public interface NeedsInjector extends Module{
    void setInjectorProvider(Provider<Injector> injectorProvider);
}
