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

import com.google.inject.Module;

import com.vaadin.guice.providers.CurrentUIProvider;
import com.vaadin.guice.providers.VaadinServiceProvider;
import com.vaadin.guice.providers.VaadinSessionProvider;
import com.vaadin.guice.testClasses.Target;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.VaadinSession;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ScopeTestBase {

    private CurrentUIProvider currentUIProvider;
    private VaadinSessionProvider vaadinSessionProvider;
    private VaadinServiceProvider vaadinServiceProvider;
    protected GuiceVaadin guiceVaadin;

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        vaadinSessionProvider = mock(VaadinSessionProvider.class);
        currentUIProvider = mock(CurrentUIProvider.class);
        vaadinServiceProvider = mock(VaadinServiceProvider.class);

        Reflections reflections = new Reflections("com.vaadin.guice.server.testClasses");

        Class<? extends Module>[] modules = new Class[]{};

        guiceVaadin = new GuiceVaadin(
                vaadinSessionProvider,
                currentUIProvider,
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

        guiceVaadin.getUiScoper().sessionInit(sessionInitEvent);
        guiceVaadin.sessionInit(sessionInitEvent);

        return vaadinSession;
    }
}
