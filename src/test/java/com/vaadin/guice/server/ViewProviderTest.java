package com.vaadin.guice.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.vaadin.guice.providers.CurrentUIProvider;
import com.vaadin.guice.providers.VaadinServiceProvider;
import com.vaadin.guice.providers.VaadinSessionProvider;
import com.vaadin.server.ServiceException;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;

import java.lang.reflect.Field;

import static com.vaadin.guice.server.ReflectionUtils.getGuiceUIClasses;
import static com.vaadin.guice.server.ReflectionUtils.getGuiceViewClasses;
import static com.vaadin.guice.server.ReflectionUtils.getViewChangeListenerClasses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

public class ViewProviderTest {

    private GuiceViewProvider viewProvider;

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        VaadinSessionProvider vaadinSessionProvider = mock(VaadinSessionProvider.class);
        CurrentUIProvider currentUIProvider = mock(CurrentUIProvider.class);
        VaadinServiceProvider vaadinServiceProvider = mock(VaadinServiceProvider.class);

        Reflections reflections = new Reflections("com.vaadin.guice.testClasses");

        final GuiceVaadin guiceVaadin = new GuiceVaadin(
                vaadinSessionProvider,
                currentUIProvider,
                vaadinServiceProvider,
                reflections, new Class[]{});

        viewProvider = guiceVaadin.getViewProvider();
    }

    @Test
    public void view_provider_get_view_name() throws ServiceException, NoSuchFieldException, IllegalAccessException {
        assertEquals(viewProvider.getViewName("view0"), "view0");
        assertEquals(viewProvider.getViewName("viewa/id1"), "viewa");
        assertEquals(viewProvider.getViewName("viewaa/id2"), "viewaa");
        assertEquals(viewProvider.getViewName("viewaaa/id3"), "viewaaa");
        assertEquals(viewProvider.getViewName("viewb"), "viewb");
        // getViewName() must return null if the view name is not handled by the view provider
        assertNull(viewProvider.getViewName("viewc"));
    }

}
