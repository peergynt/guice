package com.vaadin.guice.server;

import com.google.inject.Module;
import com.google.inject.Provider;

import com.vaadin.guice.testClasses.Target;
import com.vaadin.navigator.View;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ScopeTestBase {

    GuiceVaadin guiceVaadin;
    private Provider<VaadinSession> vaadinSessionProvider;

    @Before
    @SuppressWarnings("unckecked")
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        vaadinSessionProvider = mock(Provider.class);
        Provider<UI> currentUIProvider;
        currentUIProvider = (Provider<UI>) mock(Provider.class);
        Provider<View> currentViewProvider = (Provider<View>) mock(Provider.class);
        Provider<VaadinService> vaadinServiceProvider = (Provider<VaadinService>) mock(Provider.class);

        Reflections reflections = new Reflections("com.vaadin.guice.server.testClasses");

        Class<? extends Module>[] modules = new Class[]{};

        guiceVaadin = new GuiceVaadin(
                vaadinSessionProvider,
                currentUIProvider,
                currentViewProvider,
                vaadinServiceProvider,
                reflections,
                modules
        );
    }

    @Test //default prototype behaviour should not be affected
    public void testPrototype() throws ServiceException, NoSuchFieldException, IllegalAccessException {
        newSession();
        guiceVaadin.getUiScoper().startInitialization();
        Target target1 = guiceVaadin.assemble(Target.class);
        guiceVaadin.getUiScoper().endInitialization(target1);
        guiceVaadin.getUiScoper().startInitialization();
        newSession();
        Target target2 = guiceVaadin.assemble(Target.class);
        guiceVaadin.getUiScoper().endInitialization(target2);

        assertNotEquals(target1.getPrototype1(), target2.getPrototype1());
    }

    @Test //default singleton behaviour should not be affected
    public void testSingleton() throws ServiceException, NoSuchFieldException, IllegalAccessException {
        newSession();
        guiceVaadin.getUiScoper().startInitialization();
        Target target1 = guiceVaadin.assemble(Target.class);
        guiceVaadin.getUiScoper().endInitialization(target1);
        guiceVaadin.getUiScoper().startInitialization();
        newSession();
        Target target2 = guiceVaadin.assemble(Target.class);
        guiceVaadin.getUiScoper().endInitialization(target2);

        assertEquals(target1.getSingleton1(), target2.getSingleton1());
    }

    //different transaction-scopes should lead to a different set of transaction-scoped objects
    @Test
    public void testTransactionScopeDifferent() throws ServiceException, NoSuchFieldException, IllegalAccessException {

        newSession();
        guiceVaadin.getUiScoper().startInitialization();
        Target target1 = guiceVaadin.assemble(Target.class);
        guiceVaadin.getUiScoper().endInitialization(target1);
        guiceVaadin.getUiScoper().startInitialization();
        newSession();
        Target target2 = guiceVaadin.assemble(Target.class);
        guiceVaadin.getUiScoper().endInitialization(target2);

        assertNotNull(target1);
        assertNotNull(target2);
    }

    //a single transaction-scope should lead to the same set of transaction-scoped objects being injected
    @Test
    public void testTransactionScopeSame() throws ServiceException, NoSuchFieldException, IllegalAccessException {

        newSession();
        guiceVaadin.getUiScoper().startInitialization();
        Target target1 = guiceVaadin.assemble(Target.class);
        guiceVaadin.getUiScoper().endInitialization(target1);
        guiceVaadin.getUiScoper().startInitialization();
        newSession();
        Target target2 = guiceVaadin.assemble(Target.class);
        guiceVaadin.getUiScoper().endInitialization(target2);

        assertNotNull(target1);
        assertNotNull(target2);

        assertNotEquals(target1.getUiScoped1(), target2.getUiScoped1());
        assertNotEquals(target1.getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(target1.getUiScoped1().getUiScoped2(), target2.getUiScoped1().getUiScoped2());
    }

    void setVaadinSession(VaadinSession vaadinSession) {
        when(vaadinSessionProvider.get()).thenReturn(vaadinSession);
    }

    VaadinSession newSession() throws ServiceException {
        VaadinSession vaadinSession = mock(VaadinSession.class);

        SessionInitEvent sessionInitEvent = mock(SessionInitEvent.class);

        setVaadinSession(vaadinSession);
        when(sessionInitEvent.getSession()).thenReturn(vaadinSession);

        guiceVaadin.getViewScoper().sessionInit(sessionInitEvent);
        guiceVaadin.getUiScoper().sessionInit(sessionInitEvent);
        guiceVaadin.sessionInit(sessionInitEvent);

        return vaadinSession;
    }
}
