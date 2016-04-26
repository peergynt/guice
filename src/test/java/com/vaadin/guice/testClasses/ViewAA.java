package com.vaadin.guice.testClasses;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;

@GuiceView(name="viewaa")
public class ViewAA implements View {

    private static final long serialVersionUID = 1L;

    @Override
    public void enter(ViewChangeEvent event) {
    }

}
