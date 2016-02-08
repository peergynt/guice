package com.vaadin.guice.server;

import com.google.inject.AbstractModule;

import com.vaadin.guice.testClasses.AnImplementation;
import com.vaadin.guice.testClasses.AnInterface;

public class StaticlyLoadedModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AnInterface.class).to(AnImplementation.class);
    }
}
