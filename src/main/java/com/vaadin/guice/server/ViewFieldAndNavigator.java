package com.vaadin.guice.server;

import java.lang.reflect.Field;

class ViewFieldAndNavigator {
    private final Field viewField;
    private final Class<? extends GuiceNavigator> navigator;

    public ViewFieldAndNavigator(Field viewField, Class<? extends GuiceNavigator> navigator) {
        this.viewField = viewField;
        this.navigator = navigator;
    }

    public Field getViewField() {
        return viewField;
    }

    public Class<? extends GuiceNavigator> getNavigator() {
        return navigator;
    }
}
