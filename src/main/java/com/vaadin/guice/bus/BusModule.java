package com.vaadin.guice.bus;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;

import com.vaadin.guice.server.NeedsInjector;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The busses in com.vaadin.guice.bus can perfectly be used without this module, except of
 * the {@link GlobalEventBus}, which is an interface that needs to be bound by calling either
 * {@link BusModule#BusModule()} for the default non-distributed EventBus or by calling {@link BusModule#BusModule(Class)}
 * with your own implementation that may be distributed. Bus-registration will be wired up for you by guice.
 * If a BusModule is applied, Guice will register every instance with a @Subscribe annotation to the
 * corresponding bus via the marker interfaces in com.vaadin.guice.bus.events.
 * For example in the following code, Guice will register every new instance of Foo to the UIEventBus after creation.
 *
 * <pre>
 *     <code>
 *         public final class MyUiEvent implements UIEvent {
 *         }
 *
 *         public final class Foo {
 *             {@literal @}Subscribe
 *             public void onMyUiEvent(MyUiEvent myUiEvent){
 *                 ...
 *             }
 *         }
 *     </code>
 * </pre>
 */
public final class BusModule extends AbstractModule implements NeedsInjector {
    private final Class<? extends GlobalEventBus> globalEventBusClass;
    private Provider<Injector> injectorProvider;

    public BusModule() {
        this(GlobalEventBusImpl.class);
    }

    public BusModule(Class<? extends GlobalEventBus> globalEventBusClass) {
        this.globalEventBusClass = checkNotNull(globalEventBusClass);
    }

    @Override
    protected void configure() {
        bindListener(
                new SubscriberMethodsMatcher(),
                new SubscriptionTypeListener(injectorProvider)
        );

        bind(GlobalEventBus.class).to(globalEventBusClass);
    }

    @Override
    public void setInjectorProvider(Provider<Injector> injectorProvider) {
        this.injectorProvider = injectorProvider;
    }
}
