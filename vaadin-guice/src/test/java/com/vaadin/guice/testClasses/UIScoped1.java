package com.vaadin.guice.testClasses;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;

@UIScope
public class UIScoped1 {

    @Inject
    private UIScoped2 uiScoped2;

    @Inject
    private ViewScoped2 viewScoped2;

    public UIScoped2 getUiScoped2() {
        return uiScoped2;
    }

    public void setUiScoped2(UIScoped2 uiScoped2) {
        this.uiScoped2 = uiScoped2;
    }

    public ViewScoped2 getViewScoped2() {
        return viewScoped2;
    }

    public void setViewScoped2(ViewScoped2 viewScoped2) {
        this.viewScoped2 = viewScoped2;
    }
}
