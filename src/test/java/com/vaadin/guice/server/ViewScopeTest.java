package com.vaadin.guice.server;

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.guice.testClasses.ViewA;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

public class ViewScopeTest extends ScopeTestBase {

    @Before
    public void setup(){
    }

    @Test
    public void view_scopes_and_ui_scopes_should_not_overlap() throws NoSuchFieldException, IllegalAccessException {
        GuiceVaadin guiceVaadin = getGuiceVaadin(new VaadinServletWithDynamicLoadedModule());

        final ViewA viewA = guiceVaadin.assemble(ViewA.class);
    }

    private GuiceVaadin getGuiceVaadin(GuiceVaadinServlet servlet) throws NoSuchFieldException, IllegalAccessException {
        final Field field = servlet.getClass().getSuperclass().getDeclaredField("guiceVaadin");
        field.setAccessible(true);
        return (GuiceVaadin) field.get(servlet);
    }

    @Configuration(modules = {}, basePackages = "com.vaadin.guice.testClasses")
    private static class VaadinServletWithDynamicLoadedModule extends GuiceVaadinServlet {
    }
}
