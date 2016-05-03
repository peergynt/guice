package com.vaadin.guice.server;

import com.vaadin.guice.testClasses.Prototype1;
import com.vaadin.guice.testClasses.Singleton1;
import com.vaadin.guice.testClasses.Target;
import com.vaadin.guice.testClasses.UIScoped1;
import com.vaadin.guice.testClasses.UIScoped2;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinSession;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class UIScopeTest extends ScopeTestBase {

    @Test
    public void ui_scopes_should_not_overlap_in_same_session() throws ServiceException, NoSuchFieldException, IllegalAccessException {
        newSession();

        Target target1 = createTarget();

        Target target2 = createTarget();

        //directly
        assertEquals(target1.getUiScoped1().getUiScoped2(), target1.getUiScoped2());
        assertEquals(target2.getUiScoped1().getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(target1.getUiScoped1(), target2.getUiScoped1());
        assertNotEquals(target1.getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(target1.getUiScoped1().getUiScoped2(), target2.getUiScoped1().getUiScoped2());

        //read out the providers
        setCurrentUi(target1);
        UIScoped1 uiScoped1FromTarget1 = target1.getUiScoped1Provider().get();
        UIScoped2 uiScoped2FromTarget1 = target1.getUiScoped2Provider().get();

        setCurrentUi(target2);
        UIScoped1 uiScoped1FromTarget2 = target2.getUiScoped1Provider().get();
        UIScoped2 uiScoped2FromTarget2 = target2.getUiScoped2Provider().get();

        setCurrentUi(null);

        //via providers
        assertEquals(uiScoped1FromTarget1.getUiScoped2(), uiScoped2FromTarget1);
        assertEquals(uiScoped1FromTarget2.getUiScoped2(), uiScoped2FromTarget2);

        assertNotEquals(uiScoped1FromTarget1, uiScoped1FromTarget2);
        assertNotEquals(uiScoped2FromTarget1, uiScoped2FromTarget2);
        assertNotEquals(uiScoped1FromTarget1.getUiScoped2(), uiScoped1FromTarget2.getUiScoped2());

        //mixed
        assertEquals(uiScoped1FromTarget1.getUiScoped2(), target1.getUiScoped2());
        assertEquals(uiScoped1FromTarget2.getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(uiScoped1FromTarget1, target2.getUiScoped1());
        assertNotEquals(uiScoped2FromTarget1, target2.getUiScoped2());
        assertNotEquals(uiScoped1FromTarget1.getUiScoped2(), target2.getUiScoped1().getUiScoped2());

        assertEquals(target1.getUiScoped1().getUiScoped2(), uiScoped2FromTarget1);
        assertEquals(target2.getUiScoped1().getUiScoped2(), uiScoped2FromTarget2);
        assertNotEquals(target1.getUiScoped1(), uiScoped1FromTarget2);
        assertNotEquals(target1.getUiScoped2(), uiScoped2FromTarget2);
        assertNotEquals(target1.getUiScoped1().getUiScoped2(), uiScoped1FromTarget2.getUiScoped2());
    }

    @Test
    public void ui_scopes_should_not_overlap_in_different_sessions() throws ServiceException, NoSuchFieldException, IllegalAccessException {
        final VaadinSession vaadinSession1 = newSession();

        Target target1 = createTarget();

        final VaadinSession vaadinSession2 = newSession();

        Target target2 = createTarget();

        assertNotNull(target1);
        assertNotNull(target2);

        //directly
        assertEquals(target1.getUiScoped1().getUiScoped2(), target1.getUiScoped2());
        assertEquals(target2.getUiScoped1().getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(target1.getUiScoped1(), target2.getUiScoped1());
        assertNotEquals(target1.getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(target1.getUiScoped1().getUiScoped2(), target2.getUiScoped1().getUiScoped2());

        //read out the providers
        setVaadinSession(vaadinSession1);
        setCurrentUi(target1);

        UIScoped1 uiScoped1FromTarget1 = target1.getUiScoped1Provider().get();
        UIScoped2 uiScoped2FromTarget1 = target1.getUiScoped2Provider().get();

        setVaadinSession(vaadinSession2);
        setCurrentUi(target2);

        UIScoped1 uiScoped1FromTarget2 = target2.getUiScoped1Provider().get();
        UIScoped2 uiScoped2FromTarget2 = target2.getUiScoped2Provider().get();

        setCurrentUi(null);

        //via providers
        assertEquals(uiScoped1FromTarget1.getUiScoped2(), uiScoped2FromTarget1);
        assertEquals(uiScoped1FromTarget2.getUiScoped2(), uiScoped2FromTarget2);

        assertNotEquals(uiScoped1FromTarget1, uiScoped1FromTarget2);
        assertNotEquals(uiScoped2FromTarget1, uiScoped2FromTarget2);
        assertNotEquals(uiScoped1FromTarget1.getUiScoped2(), uiScoped1FromTarget2.getUiScoped2());

        //mixed
        assertEquals(uiScoped1FromTarget1.getUiScoped2(), target1.getUiScoped2());
        assertEquals(uiScoped1FromTarget2.getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(uiScoped1FromTarget1, target2.getUiScoped1());
        assertNotEquals(uiScoped2FromTarget1, target2.getUiScoped2());
        assertNotEquals(uiScoped1FromTarget1.getUiScoped2(), target2.getUiScoped1().getUiScoped2());

        assertEquals(target1.getUiScoped1().getUiScoped2(), uiScoped2FromTarget1);
        assertEquals(target2.getUiScoped1().getUiScoped2(), uiScoped2FromTarget2);
        assertNotEquals(target1.getUiScoped1(), uiScoped1FromTarget2);
        assertNotEquals(target1.getUiScoped2(), uiScoped2FromTarget2);
        assertNotEquals(target1.getUiScoped1().getUiScoped2(), uiScoped1FromTarget2.getUiScoped2());
    }

    @Test
    public void singletons_should_be_shared_between_ui_scopes() throws ServiceException, NoSuchFieldException, IllegalAccessException {
        newSession();

        Target target1 = createTarget();

        Target target2 = createTarget();

        assertNotNull(target1);
        assertNotNull(target2);

        final Singleton1 singleton = injector.getInstance(Singleton1.class);

        assertNotNull(singleton);
        assertEquals(singleton, target1.getSingleton1());
        assertEquals(singleton, target2.getSingleton1());

        //read out the providers
        setCurrentUi(target1);
        Singleton1 singleton1FromTarget1 = target1.getSingleton1Provider().get();

        setCurrentUi(target2);
        Singleton1 singleton1FromTarget2 = target2.getSingleton1Provider().get();

        setCurrentUi(null);

        //check the provided values
        assertEquals(singleton, singleton1FromTarget1);
        assertEquals(singleton, singleton1FromTarget2);
    }

    @Test
    public void prototypes_should_be_unique_between_ui_scopes() throws ServiceException, NoSuchFieldException, IllegalAccessException {
        newSession();

        Target target1 = createTarget();

        Target target2 = createTarget();

        assertNotNull(target1);
        assertNotNull(target2);

        assertNotEquals(target2.getPrototype1(), target1.getPrototype1());

        //read out the providers
        setCurrentUi(target1);
        Prototype1 prototype1FromTarget1 = target1.getPrototype1Provider().get();

        setCurrentUi(target2);
        Prototype1 prototype1FromTarget2 = target2.getPrototype1Provider().get();

        assertNotEquals(prototype1FromTarget1, prototype1FromTarget2);
        assertNotEquals(target1.getPrototype1(), prototype1FromTarget1);
        assertNotEquals(target2.getPrototype1(), prototype1FromTarget2);
    }

    private void setCurrentUi(Target target) {
        when(currentUIProvider.get()).thenReturn(target);
    }

    private Target createTarget() {
        uiScoper.startInitialization();
        Target target = injector.getInstance(Target.class);
        uiScoper.endInitialization(target);
        setCurrentUi(target);
        return target;
    }
}
