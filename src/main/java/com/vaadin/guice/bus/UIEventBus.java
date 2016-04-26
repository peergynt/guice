package com.vaadin.guice.bus;

import com.google.common.eventbus.EventBus;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.server.VaadinSession;

/**
 * This class serves as a means to allow UI-scope communication between objects.
 * UIEventBus is intended for events that are of 'UI-scope' interest, like updates to
 * data that is used by only the current UI. It is UI-scoped and therefore
 * is not prone to memory leaks.
 *
 * <code>
 * {@literal @}Inject
 * private UIEventBus uiIEventBus;
 *
 * ...
 * uiIEventBus.post(new DataSetInUIScopeChangedEvent());
 * ...
 *
 * </code>
 * </pre>
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@UIScope
@SuppressWarnings("unused")
public final class UIEventBus extends EventBus{
    UIEventBus(){
    }
}
