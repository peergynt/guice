package com.vaadin.guice.server;

import com.google.inject.Guice;
import com.google.inject.Injector;

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

public class ViewProviderTest {

    protected CurrentUIProvider currentUIProvider;
    protected UIScoper uiScoper;
    protected GuiceViewProvider viewProvider;
    protected Injector injector;
    private SessionProvider sessionProvider;

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        sessionProvider = mock(SessionProvider.class);
        currentUIProvider = mock(CurrentUIProvider.class);

        Reflections reflections = new Reflections("com.vaadin.guice.testClasses");

        VaadinModule vaadinModule = new VaadinModule(
                sessionProvider,
                getGuiceViewClasses(reflections),
                getGuiceUIClasses(reflections),
                getViewChangeListenerClasses(reflections),
                currentUIProvider);

        injector = Guice.createInjector(vaadinModule);

        final Field viewProviderField = VaadinModule.class.getDeclaredField("viewProvider");
        viewProviderField.setAccessible(true);
        viewProvider = (GuiceViewProvider) viewProviderField.get(vaadinModule);
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
