package com.vaadin.guice.server;

import com.google.inject.Provider;

import com.vaadin.navigator.Navigator;
import com.vaadin.ui.UI;

@Deprecated //use UI.getCurrent().getNavigator()
public class NavigatorProvider implements Provider<Navigator> {
    @Override
    public Navigator get() {
        return UI.getCurrent().getNavigator();
    }
}
