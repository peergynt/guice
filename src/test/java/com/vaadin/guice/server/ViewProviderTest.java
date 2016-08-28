package com.vaadin.guice.server;

import com.google.inject.Provider;

import com.vaadin.navigator.View;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class ViewProviderTest {

    private GuiceViewProvider viewProvider;

    @Before
    @SuppressWarnings("unckecked")
    public void setup() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, InstantiationException {

        Provider<VaadinSession> vaadinSessionProvider = (Provider<VaadinSession>) mock(Provider.class);
        Provider<UI> currentUIProvider = (Provider<UI>) mock(Provider.class);
        Provider<View> currentViewProvider = (Provider<View>) mock(Provider.class);
        Provider<VaadinService> vaadinServiceProvider = (Provider<VaadinService>) mock(Provider.class);

        Reflections reflections = new Reflections("com.vaadin.guice.testClasses");

        final GuiceVaadin guiceVaadin = new GuiceVaadin(
                vaadinSessionProvider,
                currentUIProvider,
                currentViewProvider,
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
