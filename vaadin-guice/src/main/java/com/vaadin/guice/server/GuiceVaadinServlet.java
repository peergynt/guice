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

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * Subclass of the standard {@link com.vaadin.server.VaadinServlet Vaadin
 * servlet} that adds a {@link GuiceUIProvider} to every new Vaadin session and
 * allows the use of a custom service URL on the bootstrap page.
 * <p>
 * If you need a custom Vaadin servlet, you can either extend this servlet
 * directly, or extend another subclass of {@link VaadinServlet} and just add
 * the UI provider.
 * <p>
 * This servlet also implements a hack to get around the behavior of guice
 * ServletForwardingController/ServletWrappingController. Those controllers
 * return null as the pathInfo of requests forwarded to the Vaadin servlet, and
 * use the mapping as the servlet path whereas with Vaadin the mapping typically
 * corresponds to a UI, not a virtual servlet. Thus, there is an option to clear
 * the servlet path in requests and compute pathInfo accordingly. This is used
 * by Vaadin guice Boot to make it easier to use Vaadin and guice MVC
 * applications together in the same global "namespace".
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Josh Long (josh@joshlong.com)
 */
public class GuiceVaadinServlet extends VaadinServlet {

    private static final long serialVersionUID = 5371983676318947478L;
    private final VaadinModule vaadinModule;
    private String serviceUrlPath = null;

    public GuiceVaadinServlet() {
        Configuration annotation = getClass().getAnnotation(Configuration.class);

        if (annotation == null) {
            throw new IllegalStateException("GuiceVaadinServlet cannot be used without 'Configuration' annotation");
        }

        vaadinModule = new VaadinModule(annotation);

        InjectorHolder.setInjector(Guice.createInjector(vaadinModule));
    }

    @Override
    protected void servletInitialized() throws ServletException {
        vaadinModule.vaadinInitialized();
    }

    /**
     * Return the path of the service URL (URL for all client-server
     * communication) relative to the context path. A value of null means that
     * the default service path of Vaadin should be used. The path should start
     * with a slash.
     *
     * @return service URL path relative to context path (starting with slash)
     *         or null to use the default
     */
    public String getServiceUrlPath() {
        return serviceUrlPath;
    }

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        // this is needed when using a custom service URL
        GuiceVaadinServletService service = new GuiceVaadinServletService(
                this, deploymentConfiguration, getServiceUrlPath());
        service.init();
        return service;
    }

    @Override
    protected VaadinServletRequest createVaadinRequest(
            HttpServletRequest request) {
        if (serviceUrlPath != null) {
            return new GuiceVaadinServletRequest(request, getService(), true);
        } else {
            return new VaadinServletRequest(request, getService());
        }
    }
}
