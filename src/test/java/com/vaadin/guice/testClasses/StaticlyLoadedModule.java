package com.vaadin.guice.testClasses;

import com.google.inject.AbstractModule;

public class StaticlyLoadedModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AnInterface.class).to(AnImplementation.class);
        bind(AnotherInterface.class).to(AnotherInterfaceImplementation.class);
    }
}
