package com.vaadin.guice.server;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.SingleComponentContainer;
import com.vaadin.ui.UI;

public class GuiceNavigator extends Navigator {

    private static final long serialVersionUID = -2693174603614667628L;

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
