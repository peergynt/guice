package com.vaadin.guice.bus;

public interface GlobalEventBus {
    void register(Object object);

    void unregister(Object object);

    void post(Object object);
}
