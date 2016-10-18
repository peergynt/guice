package com.vaadin.guice.bus;

/**
 * This interface serves as a means to allow application-scope communication between objects.
 * GlobalEventBusImpl is intended for events that are of 'global' interest, like updates to data
 * that is used by multiple UIs simultaneously. It is singleton-scoped and will release any
 * subscribers once their {@link com.vaadin.server.VaadinSession} is ended in order to prevent memory leaks.
 * In order to use GlobalEventBus, a {@link BusModule} needs to be installed.
 *
 * <code> {@literal @}Inject private GlobalEventBusImpl globalEventBus;
 *
 * ... globalEventBus.post(new DataSetOfGlobalInterestChangedEvent()); ...
 *
 * </code> </pre>
 *
 * This is an abstraction for the global event-bus. If you don't supply your own implementation
 * via {@link BusModule#BusModule(Class)}, a subclass of {@link com.google.common.eventbus.EventBus} will be
 * bound, that takes care of cleaning up after session end.
 *
 * * @author Bernd Hopp (bernd@vaadin.com)
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
