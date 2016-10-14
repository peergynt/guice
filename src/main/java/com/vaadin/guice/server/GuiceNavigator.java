package com.vaadin.guice.server;

import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.SingleComponentContainer;
import com.vaadin.ui.UI;

/**
 * subclass GuiceNavigator if you need a custom navigator. To use,
 * set your GuiceNavigator's class in {@link GuiceUI#navigator()}
 */
public class GuiceNavigator extends Navigator {

    void init(UI ui, ComponentContainer container) {
        init(ui, new ComponentContainerViewDisplay(container));
    }

    void init(UI ui, SingleComponentContainer container) {
        init(ui, new SingleComponentContainerViewDisplay(container));
    }

    void init(UI ui, ViewDisplay display) {
        init(ui, new UriFragmentManager(ui.getPage()), display);
    }

}
