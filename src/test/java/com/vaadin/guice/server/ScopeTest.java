/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.guice.server;

import com.google.inject.Guice;
import com.google.inject.Injector;

import com.vaadin.guice.testClasses.Target;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.VaadinSession;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScopeTest {

    private SessionProvider sessionProvider;
    private SessionBasedScoper uiScoper;
    private TransactionBasedScoper transactionBasedScoper;
    private GuiceUIProvider uiProvider;
    private Injector injector;

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        sessionProvider = mock(SessionProvider.class);
        VaadinModule vaadinModule = new VaadinModule(sessionProvider, "com.vaadin.guice.server.testClasses");
        injector = Guice.createInjector(vaadinModule);
        final Field uiScoperField = VaadinModule.class.getDeclaredField("uiScoper");
        uiScoperField.setAccessible(true);
        this.uiScoper = (SessionBasedScoper) uiScoperField.get(vaadinModule);
        final Field viewScoperField = VaadinModule.class.getDeclaredField("viewScoper");
        viewScoperField.setAccessible(true);
        transactionBasedScoper = (TransactionBasedScoper) viewScoperField.get(vaadinModule);


        final Field uiProviderField = VaadinModule.class.getDeclaredField("uiProvider");
        uiProviderField.setAccessible(true);
        uiProvider = (GuiceUIProvider) uiProviderField.get(vaadinModule);
    }

    @Test //default prototype behaviour should not be affected
    public void testPrototype() throws ServiceException, NoSuchFieldException, IllegalAccessException {
        newSession();
        transactionBasedScoper.startTransaction();
        Target target1 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        newSession();
        transactionBasedScoper.startTransaction();
        Target target2 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        assertNotEquals(target1.getPrototype1(), target2.getPrototype1());
    }

    @Test //default singleton behaviour should not be affected
    public void testSingleton() throws ServiceException, NoSuchFieldException, IllegalAccessException {
        System.err.println("jarkz!");

        newSession();
        transactionBasedScoper.startTransaction();
        Target target1 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        newSession();
        transactionBasedScoper.startTransaction();
        Target target2 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        assertEquals(target1.getSingleton1(), target2.getSingleton1());
    }

    //different session-scoped should lead to a different set of session-scoped objects
    @Test
    public void testSessionScopeDifferent() throws ServiceException, NoSuchFieldException, IllegalAccessException {

        newSession();
        transactionBasedScoper.startTransaction();
        Target target1 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        newSession();
        transactionBasedScoper.startTransaction();
        Target target2 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        assertNotNull(target1);
        assertNotNull(target2);
        assertEquals(target1.getUiScoped1().getUiScoped2(), target1.getUiScoped2());
        assertEquals(target2.getUiScoped1().getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(target1.getUiScoped1(), target2.getUiScoped1());
        assertNotEquals(target1.getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(target1.getUiScoped1().getUiScoped2(), target2.getUiScoped1().getUiScoped2());
    }

    //a single session-scope should lead to the same set of session-scoped objects being injected
    @Test
    public void testSessionScopeSame() throws ServiceException, NoSuchFieldException, IllegalAccessException {

        newSession();
        transactionBasedScoper.startTransaction();
        Target target1 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        transactionBasedScoper.startTransaction();
        Target target2 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        assertNotNull(target1);
        assertNotNull(target2);
        assertEquals(target1.getUiScoped1().getUiScoped2(), target1.getUiScoped2());
        assertEquals(target2.getUiScoped1().getUiScoped2(), target2.getUiScoped2());
        assertEquals(target1.getUiScoped1(), target2.getUiScoped1());
        assertEquals(target1.getUiScoped2(), target2.getUiScoped2());
        assertEquals(target1.getUiScoped1().getUiScoped2(), target2.getUiScoped1().getUiScoped2());
        assertNotEquals(target1.getViewScoped1(), target2.getViewScoped1());
        assertNotEquals(target1.getViewScoped2(), target2.getViewScoped2());
        assertNotEquals(target1.getViewScoped1().getViewScoped2(), target2.getViewScoped1().getViewScoped2());
    }

    //different transaction-scopes should lead to a different set of transaction-scoped objects
    @Test
    public void testTransactionScopeDifferent() throws ServiceException, NoSuchFieldException, IllegalAccessException {

        newSession();
        transactionBasedScoper.startTransaction();
        Target target1 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        newSession();
        transactionBasedScoper.startTransaction();
        Target target2 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        assertNotNull(target1);
        assertNotNull(target2);

        assertEquals(target1.getViewScoped1().getViewScoped2(), target1.getViewScoped2());
        assertEquals(target2.getViewScoped1().getViewScoped2(), target2.getViewScoped2());
        assertNotEquals(target1.getViewScoped1(), target2.getViewScoped1());
        assertNotEquals(target1.getViewScoped2(), target2.getViewScoped2());
        assertNotEquals(target1.getViewScoped1().getViewScoped2(), target2.getViewScoped1().getViewScoped2());
    }


    //a single transaction-scope should lead to the same set of transaction-scoped objects being injected
    @Test
    public void testTransactionScopeSame() throws ServiceException, NoSuchFieldException, IllegalAccessException {

        newSession();
        transactionBasedScoper.startTransaction();
        Target target1 = injector.getInstance(Target.class);

        newSession();
        Target target2 = injector.getInstance(Target.class);
        transactionBasedScoper.endTransaction();

        assertNotNull(target1);
        assertNotNull(target2);

        assertEquals(target1.getViewScoped1().getViewScoped2(), target1.getViewScoped2());
        assertEquals(target2.getViewScoped1().getViewScoped2(), target2.getViewScoped2());
        assertEquals(target1.getViewScoped1(), target2.getViewScoped1());
        assertEquals(target1.getViewScoped2(), target2.getViewScoped2());
        assertEquals(target1.getViewScoped1().getViewScoped2(), target2.getViewScoped1().getViewScoped2());
        assertNotEquals(target1.getUiScoped1(), target2.getUiScoped1());
        assertNotEquals(target1.getUiScoped2(), target2.getUiScoped2());
        assertNotEquals(target1.getUiScoped1().getUiScoped2(), target2.getUiScoped1().getUiScoped2());
    }

    private void newSession() throws ServiceException {
        VaadinSession vaadinSession = mock(VaadinSession.class);

        SessionInitEvent sessionInitEvent = mock(SessionInitEvent.class);

        when(sessionProvider.getCurrentSession()).thenReturn(vaadinSession);

        when(sessionInitEvent.getSession()).thenReturn(vaadinSession);

        uiScoper.sessionInit(sessionInitEvent);
        uiProvider.sessionInit(sessionInitEvent);
    }
}
