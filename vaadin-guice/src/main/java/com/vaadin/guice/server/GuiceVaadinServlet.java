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
import com.google.inject.Module;

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * Subclass of the standard {@link com.vaadin.server.VaadinServlet Vaadin
 * servlet} that adds a {@link GuiceUIProvider} to every new Vaadin session
 * @author Petter Holmström (petter@vaadin.com)
 * @author Josh Long (josh@joshlong.com)
 * @author Bernd Hopp (bernd@vaadin.com)
 */
public class GuiceVaadinServlet extends VaadinServlet {

    private final VaadinModule vaadinModule;

    public GuiceVaadinServlet() {
        Configuration annotation = getClass().getAnnotation(Configuration.class);

        if (annotation == null) {
            throw new IllegalStateException("GuiceVaadinServlet cannot be used without 'Configuration' annotation");
        }

        List<Module> modules = new ArrayList<Module>(annotation.modules().length + 1);

        for (Class<? extends Module> moduleClass : annotation.modules()) {
            try {
                modules.add(moduleClass.newInstance());
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

        this.vaadinModule = new VaadinModule(sessionProvider, annotation.basePackage());

        modules.add(vaadinModule);

        InjectorHolder.setInjector(Guice.createInjector(modules));
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

    @Override
    protected VaadinServletRequest createVaadinRequest(
            HttpServletRequest request) {
           return new VaadinServletRequest(request, getService());
    }
}
