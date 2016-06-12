package com.vaadin.guice.server;

import com.google.inject.Provider;

import com.vaadin.navigator.View;
import com.vaadin.server.VaadinSession;

class ViewScoper extends ScoperBase<View> {

    ViewScoper(Provider<VaadinSession> vaadinSessionProvider, Provider<View> currentViewProvider) {
        super(currentViewProvider, vaadinSessionProvider);
    }
}
