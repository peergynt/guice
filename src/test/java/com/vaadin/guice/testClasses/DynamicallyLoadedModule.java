package com.vaadin.guice.testClasses;

import com.google.inject.AbstractModule;

import com.vaadin.guice.annotation.UIModule;

@UIModule
public class DynamicallyLoadedModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AnInterface.class).to(ASecondImplementation.class);
    }
}
