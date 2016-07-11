package com.vaadin.guice.testClasses;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.ViewScope;

@ViewScope
public class ViewScoped1 {

    @Inject
    private ViewScoped2 uiScoped2;

    public ViewScoped2 getViewScoped2() {
        return uiScoped2;
    }

    public void setViewScoped2(ViewScoped2 uiScoped2) {
        this.uiScoped2 = uiScoped2;
    }
}
