package com.vaadin.guice.testClasses;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;

@GuiceView(name = "viewa")
public class ViewA implements View {

    private static final long serialVersionUID = 1L;

    @Override
    public void enter(ViewChangeEvent event) {
    }

    @Inject
    private ViewScoped1 viewScoped1;

    @Inject
    private ViewScoped2 viewScoped2;

    public ViewScoped1 getViewScoped1() {
        return viewScoped1;
    }

    public ViewScoped2 getViewScoped2() {
        return viewScoped2;
    }
}
