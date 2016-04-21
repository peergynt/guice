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
import com.google.inject.Module;

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.inject.util.Modules.override;
import static com.vaadin.guice.server.ReflectionUtils.getGuiceUIClasses;
import static com.vaadin.guice.server.ReflectionUtils.getGuiceViewClasses;
import static com.vaadin.guice.server.ReflectionUtils.getUIModules;
import static com.vaadin.guice.server.ReflectionUtils.getViewChangeListenerClasses;

/**
 * Subclass of the standard {@link com.vaadin.server.VaadinServlet Vaadin servlet} that adds a
 * {@link GuiceUIProvider} to every new Vaadin session
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Josh Long (josh@joshlong.com)
 * @author Bernd Hopp (bernd@vaadin.com)
 */
public class GuiceVaadinServlet extends VaadinServlet {

    private final VaadinModule vaadinModule;

    public GuiceVaadinServlet() {
        Configuration annotation = getClass().getAnnotation(Configuration.class);

        checkArgument(
                annotation != null,
                "GuiceVaadinServlet cannot be used without 'Configuration' annotation"
        );

        checkArgument(
                annotation.basePackages().length > 0,
                "at least on 'basePackages'-parameter expected in Configuration of " + getClass()
        );

        List<Module> hardWiredModules = new ArrayList<Module>(annotation.modules().length);

        for (Class<? extends Module> moduleClass : annotation.modules()) {
            try {
                hardWiredModules.add(moduleClass.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        SessionProvider sessionProvider = new SessionProvider() {
            @Override
            public VaadinSession getCurrentSession() {
                return VaadinSession.getCurrent();
            }
        };

        CurrentUIProvider currentUIProvider = new CurrentUIProvider() {
            @Override
            public UI getCurrentUI() {
                return UI.getCurrent();
            }
        };

        Reflections reflections = new Reflections(annotation.basePackages());

        Set<Class<? extends UI>> uis = getGuiceUIClasses(reflections);

        Set<Class<? extends View>> views = getGuiceViewClasses(reflections);

        Set<Class<? extends ViewChangeListener>> viewChangeListeners = getViewChangeListenerClasses(reflections);

        Set<Module> dynamicallyLoadedModules = getUIModules(reflections);

        this.vaadinModule = new VaadinModule(sessionProvider, views, uis, viewChangeListeners, currentUIProvider);

        Module combinedModule = override(hardWiredModules).with(dynamicallyLoadedModules);

        InjectorHolder.setInjector(Guice.createInjector(vaadinModule, combinedModule));
    }

    protected Injector getInjector() {
        return InjectorHolder.getInjector();
    }

    @Override
    protected void servletInitialized() throws ServletException {
        vaadinModule.vaadinInitialized(VaadinService.getCurrent());
    }

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        // this is needed when using a custom service URL
        GuiceVaadinServletService service = new GuiceVaadinServletService(
                this, deploymentConfiguration, null);
        service.init();
        return service;
    }
}
