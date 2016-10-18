package com.vaadin.guice.server;

import com.google.inject.ConfigurationException;

import com.vaadin.guice.annotation.GuiceVaadinConfiguration;
import com.vaadin.guice.testClasses.ASecondImplementation;
import com.vaadin.guice.testClasses.AnImplementation;
import com.vaadin.guice.testClasses.AnInterface;
import com.vaadin.guice.testClasses.AnotherInterface;
import com.vaadin.guice.testClasses.AnotherInterfaceImplementation;
import com.vaadin.guice.testClasses.StaticlyLoadedModule;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UIModuleTest {

    @Test
    public void dynamically_loaded_modules_should_override() throws NoSuchFieldException, IllegalAccessException {
        GuiceVaadin guiceVaadin = getGuiceVaadin(new VaadinServletWithStaticAndDynamicLoadedModules());

        AnInterface anInterface = guiceVaadin.assemble(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof ASecondImplementation);

        AnotherInterface anotherInterface = guiceVaadin.assemble(AnotherInterface.class);

        assertNotNull(anotherInterface);
        assertTrue(anotherInterface instanceof AnotherInterfaceImplementation);
    }

    @Test
    public void statically_loaded_modules_should_be_considered() throws NoSuchFieldException, IllegalAccessException {
        GuiceVaadin guiceVaadin = getGuiceVaadin(new VaadinServletWithStaticLoadedModule());

        AnInterface anInterface = guiceVaadin.assemble(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof AnImplementation);

        AnotherInterface anotherInterface = guiceVaadin.assemble(AnotherInterface.class);

        assertNotNull(anotherInterface);
        assertTrue(anotherInterface instanceof AnotherInterfaceImplementation);
    }

    @Test
    public void dynamically_loaded_modules_should_be_considered() throws NoSuchFieldException, IllegalAccessException {
        GuiceVaadin guiceVaadin = getGuiceVaadin(new VaadinServletWithDynamicLoadedModule());

        AnInterface anInterface = guiceVaadin.assemble(AnInterface.class);

        assertNotNull(anInterface);
        assertTrue(anInterface instanceof ASecondImplementation);
    }

    @Test(expected = ConfigurationException.class)
    public void unbound_classes_should_not_be_available() throws NoSuchFieldException, IllegalAccessException {
        GuiceVaadin guiceVaadin = getGuiceVaadin(new VaadinServletWithDynamicLoadedModule());

        guiceVaadin.assemble(AnotherInterface.class);
    }

    private GuiceVaadin getGuiceVaadin(GuiceVaadinServlet servlet) throws NoSuchFieldException, IllegalAccessException {
        final Field field = servlet.getClass().getSuperclass().getDeclaredField("guiceVaadin");
        field.setAccessible(true);
        return (GuiceVaadin) field.get(servlet);
    }

    @GuiceVaadinConfiguration(modules = {StaticlyLoadedModule.class}, basePackages = "com.vaadin.guice.testClasses")
    private static class VaadinServletWithStaticAndDynamicLoadedModules extends GuiceVaadinServlet {
    }

    @GuiceVaadinConfiguration(modules = {StaticlyLoadedModule.class}, basePackages = "com.vaadin.guice.server")
    private static class VaadinServletWithStaticLoadedModule extends GuiceVaadinServlet {
    }

    @GuiceVaadinConfiguration(modules = {}, basePackages = "com.vaadin.guice.testClasses")
    private static class VaadinServletWithDynamicLoadedModule extends GuiceVaadinServlet {
    }
}
