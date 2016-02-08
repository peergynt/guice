package com.vaadin.guice.server;

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.guice.testClasses.AnImplementation;
import com.vaadin.guice.testClasses.AnInterface;
import com.vaadin.guice.testClasses.AnotherImplementation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UIModuleTest {

    @Configuration(modules = {StaticlyLoadedModule.class}, basePackage = "com.vaadin.guice.testClasses")
    private static class VaadinServletWithStaticAndDynamicLoadedModules extends GuiceVaadinServlet{
    }

    @Configuration(modules = {StaticlyLoadedModule.class}, basePackage = "com.vaadin.guice.server")
    private static class VaadinServletWithStaticLoadedModule extends GuiceVaadinServlet{
    }

    @Configuration(modules = {}, basePackage = "com.vaadin.guice.testClasses")
    private static class VaadinServletWithDynamicLoadedModule extends GuiceVaadinServlet{
    }

    @Before
    public void init(){
        InjectorHolder.setInjector(null);
    }

    @Test
    public void dynamicly_loaded_modules_should_override(){
        VaadinServletWithStaticAndDynamicLoadedModules vaadinServletWithStaticAndDynamicLoadedModules = new VaadinServletWithStaticAndDynamicLoadedModules();

        AnInterface anInterface = InjectorHolder.getInjector().getInstance(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof AnotherImplementation);
    }

    @Test
    public void staticly_loaded_modules_should_be_considered(){
        VaadinServletWithStaticLoadedModule vaadinServletWithStaticLoadedModule = new VaadinServletWithStaticLoadedModule();

        AnInterface anInterface = InjectorHolder.getInjector().getInstance(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof AnImplementation);
    }

    @Test
    public void dynamicly_loaded_modules_should_be_considered(){
        VaadinServletWithDynamicLoadedModule vaadinServletWithDynamicLoadedModule = new VaadinServletWithDynamicLoadedModule();

        AnInterface anInterface = InjectorHolder.getInjector().getInstance(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof AnotherImplementation);
    }
}
