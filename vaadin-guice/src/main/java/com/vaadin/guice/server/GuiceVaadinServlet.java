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

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.guice.annotation.ViewScope;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.DefaultUIProvider;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;

import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

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
    private final SessionBasedScoper uiScoper = new SessionBasedScoper();
    private final TransactionBasedScoper viewScoper = new TransactionBasedScoper();
    private final GuiceViewProvider viewProvider;
    private final GuiceUIProvider uiProvider;

    private String serviceUrlPath = null;

    public GuiceVaadinServlet() {
        Configuration annotation = getClass().getAnnotation(Configuration.class);

        if (annotation == null) {
            throw new IllegalStateException("GuiceVaadinServlet cannot be used without 'Configuration' annotation");
        }

        Reflections reflections = new Reflections(annotation.basePackage());

        Set<Class<?>> uis = reflections.getTypesAnnotatedWith(GuiceUI.class);

        checkArgument(!uis.isEmpty(), "no @GuiceUI annotated UI's found in %s", annotation.basePackage());

        Set<Class<?>> uiScopedElements = reflections.getTypesAnnotatedWith(UIScope.class);
        Set<Class<?>> views = reflections.getTypesAnnotatedWith(GuiceView.class);

        checkNoUIViewScopeIntersection(uiScopedElements, views);

        viewProvider = new GuiceViewProvider(views, viewScoper);
        uiProvider = new GuiceUIProvider(uis);

        Module scopeModule = new AbstractModule() {
            @Override
            protected void configure() {
                bindScope(UIScope.class, uiScoper);
                bindScope(GuiceView.class, viewScoper);
                bindScope(ViewScope.class, viewScoper);
                bind(UIProvider.class).toInstance(uiProvider);
                bind(ViewProvider.class).toInstance(viewProvider);
            }
        };

        List<Module> modules = new ArrayList<Module>();

        modules.add(scopeModule);

        for (Class<? extends Module> aClass : annotation.modules()) {
            try {
                modules.add(aClass.newInstance());
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        InjectorHolder.setInjector(Guice.createInjector(modules));
    }

    private void checkNoUIViewScopeIntersection(Set<Class<?>> uiScopedElements, Set<Class<?>> views) {
        Sets.SetView<Class<?>> viewsWithUIScope = Sets.intersection(views, uiScopedElements);

        if (!viewsWithUIScope.isEmpty()) {
            throw new IllegalArgumentException(
                    format(
                            "@UIScope and @GuiceView are mutually exclusive because they expect different guice-scopes, please remove @UIScope from %s",
                            Joiner.on(",").join(viewsWithUIScope)
                    )
            );
        }
    }

    @Override
    protected void servletInitialized() throws ServletException {
        getService().addSessionInitListener(new SessionInitListener() {

            private static final long serialVersionUID = -6307820453486668084L;

            @Override
            public void sessionInit(SessionInitEvent sessionInitEvent)
                    throws ServiceException {
                // remove DefaultUIProvider instances to avoid mapping
                // extraneous UIs if e.g. a servlet is declared as a nested
                // class in a UI class
                VaadinSession session = sessionInitEvent.getSession();
                List<UIProvider> uiProviders = new ArrayList<UIProvider>(
                        session.getUIProviders());
                for (UIProvider provider : uiProviders) {
                    // use canonical names as these may have been loaded with
                    // different classloaders
                    if (DefaultUIProvider.class.getCanonicalName().equals(
                            provider.getClass().getCanonicalName())) {
                        session.removeUIProvider(provider);
                    }
                }

                // add guice UI provider
                session.addUIProvider(uiProvider);
            }
        });

        getService().addSessionDestroyListener(uiScoper);
        getService().addSessionInitListener(uiScoper);
        getService().addSessionDestroyListener(viewProvider);
        getService().addSessionInitListener(viewProvider);
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
