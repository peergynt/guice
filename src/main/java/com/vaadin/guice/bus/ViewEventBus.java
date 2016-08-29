package com.vaadin.guice.bus;

import com.google.common.eventbus.EventBus;

import com.vaadin.guice.annotation.ViewScope;

/**
 * This class serves as a means to allow View-scope communication between objects. ViewEventBus is
 * intended for events that are of 'View-scope' interest, like updates to data that is used by the
 * same {@link com.vaadin.navigator.View}.
 *
 * <code> {@literal @}Inject private ViewEventBus viewEventBus;
 *
 * ... viewEventBus.post(new DataSetInViewScopeChangedEvent()); ...
 *
 * </code> </pre>
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@ViewScope
public final class ViewEventBus extends EventBus {
    ViewEventBus() {
    }
}