package com.vaadin.guice.bus;

/**
 * An abstraction for the global event-bus. If you don't supply your own implementation via {@link
 * BusModule#BusModule(Class)}, a standard {@link com.google.common.eventbus.EventBus} will be
 * bound, that takes care of cleaning up after session end.
 */
public interface GlobalEventBus {
    /**
     * see {@link com.google.common.eventbus.EventBus#register(Object)}
     */
    void register(Object object);

    /**
     * see {@link com.google.common.eventbus.EventBus#unregister(Object)}
     */
    void unregister(Object object);

    /**
     * see {@link com.google.common.eventbus.EventBus#post(Object)}
     */
    void post(Object object);
}
