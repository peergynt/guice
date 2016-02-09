package com.vaadin.guice.server;

import com.google.inject.ConfigurationException;

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.guice.testClasses.AnImplementation;
import com.vaadin.guice.testClasses.AnInterface;
import com.vaadin.guice.testClasses.ASecondImplementation;
import com.vaadin.guice.testClasses.AnotherInterface;
import com.vaadin.guice.testClasses.AnotherInterfaceImplementation;
import com.vaadin.guice.testClasses.StaticlyLoadedModule;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UIModuleTest {

    @Configuration(modules = {StaticlyLoadedModule.class}, basePackages = "com.vaadin.guice.testClasses")
    private static class VaadinServletWithStaticAndDynamicLoadedModules extends GuiceVaadinServlet{
    }

    @Configuration(modules = {StaticlyLoadedModule.class}, basePackages = "com.vaadin.guice.server")
    private static class VaadinServletWithStaticLoadedModule extends GuiceVaadinServlet{
    }

    @Configuration(modules = {}, basePackages = "com.vaadin.guice.testClasses")
    private static class VaadinServletWithDynamicLoadedModule extends GuiceVaadinServlet{
    }

    @Before
    public void init(){
        InjectorHolder.setInjector(null);
    }

    @Test
    public void dynamically_loaded_modules_should_override(){
        new VaadinServletWithStaticAndDynamicLoadedModules();

        AnInterface anInterface = InjectorHolder.getInjector().getInstance(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof ASecondImplementation);

        AnotherInterface anotherInterface = InjectorHolder.getInjector().getInstance(AnotherInterface.class);

        assertNotNull(anotherInterface);
        assertTrue(anotherInterface instanceof AnotherInterfaceImplementation);
    }

    @Test
    public void statically_loaded_modules_should_be_considered(){
        new VaadinServletWithStaticLoadedModule();

        AnInterface anInterface = InjectorHolder.getInjector().getInstance(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof AnImplementation);

        AnotherInterface anotherInterface = InjectorHolder.getInjector().getInstance(AnotherInterface.class);

        assertNotNull(anotherInterface);
        assertTrue(anotherInterface instanceof AnotherInterfaceImplementation);
    }

    @Test
    public void dynamically_loaded_modules_should_be_considered(){
        new VaadinServletWithDynamicLoadedModule();

        AnInterface anInterface = InjectorHolder.getInjector().getInstance(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof ASecondImplementation);
    }

    @Test(expected = ConfigurationException.class)
    public void unbound_classes_should_not_be_available(){
        new VaadinServletWithDynamicLoadedModule();
        InjectorHolder.getInjector().getInstance(AnotherInterface.class);
    }
}
