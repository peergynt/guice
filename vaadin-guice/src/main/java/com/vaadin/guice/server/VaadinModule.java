package com.vaadin.guice.server;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Module;

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.guice.annotation.ViewScope;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.DefaultUIProvider;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;

import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Joiner.on;
import static java.lang.String.format;

class VaadinModule extends AbstractModule implements SessionInitListener {

    private static final String SCOPE_INTERSECTION_ERROR_MESSAGE = "@UIScope and @GuiceView are mutually exclusive because they expect different guice-scopes, please remove @UIScope from %s";
    private final GuiceViewProvider viewProvider;
    private final GuiceUIProvider uiProvider;
    private final SessionBasedScoper uiScoper;
    private final TransactionBasedScoper viewScoper;

    public VaadinModule(Configuration configuration) {
        Reflections reflections = new Reflections(configuration.basePackage());

        Set<Class<?>> uis = reflections.getTypesAnnotatedWith(GuiceUI.class);

        Set<Class<?>> uiScopedElements = reflections.getTypesAnnotatedWith(UIScope.class);
        Set<Class<?>> views = reflections.getTypesAnnotatedWith(GuiceView.class);

        checkNoUIViewScopeIntersection(uiScopedElements, views);

        viewScoper = new TransactionBasedScoper();
        uiScoper = new SessionBasedScoper();
        viewProvider = new GuiceViewProvider(views, viewScoper);
        uiProvider = new GuiceUIProvider(uis);

        for (Class<? extends Module> moduleClass : configuration.modules()) {
            try {
                install(moduleClass.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void checkNoUIViewScopeIntersection(Set<Class<?>> uiScopedElements, Set<Class<?>> views) {
        Sets.SetView<Class<?>> viewsWithUIScope = Sets.intersection(views, uiScopedElements);

        if (!viewsWithUIScope.isEmpty()) {
            final String errorMessage = format(SCOPE_INTERSECTION_ERROR_MESSAGE, on(",").join(viewsWithUIScope));
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    protected void configure() {
        bindScope(UIScope.class, uiScoper);
        bindScope(GuiceView.class, viewScoper);
        bindScope(ViewScope.class, viewScoper);
        bind(UIProvider.class).toInstance(uiProvider);
        bind(ViewProvider.class).toInstance(viewProvider);
    }

    public void vaadinInitialized() {
        VaadinService service = VaadinService.getCurrent();

        service.addSessionInitListener(new SessionInitListener() {
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
            }
        });

        service.addSessionDestroyListener(uiScoper);
        service.addSessionInitListener(uiScoper);
        service.addSessionDestroyListener(viewProvider);
        service.addSessionInitListener(viewProvider);
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        event.getSession().addUIProvider(uiProvider);
    }
}
