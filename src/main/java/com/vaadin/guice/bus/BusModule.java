package com.vaadin.guice.bus;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;

import com.vaadin.guice.server.NeedsInjector;

/**
 * The busses in com.vaadin.guice.bus can perfectly be used without this module, install it if you
 * want bus-registration be wired up for you by guice. If a BusModule is applied, Guice will
 * register every instance with a @Subscribe annotation to the corresponding bus via the marker
 * interfaces in com.vaadin.guice.bus.events. For example in the following code, Guice will register
 * every new instance of Foo to the UIEventBus after creation.
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
    private Provider<Injector> injectorProvider;

    @Override
    protected void configure() {
        bindListener(
                new SubscriberMethodsMatcher(),
                new SubscriptionTypeListener(injectorProvider)
        );
    }

    @Override
    public void setInjectorProvider(Provider<Injector> injectorProvider) {
        this.injectorProvider = injectorProvider;
    }

}
