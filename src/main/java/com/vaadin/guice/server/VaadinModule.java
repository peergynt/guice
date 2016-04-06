package com.vaadin.guice.server;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import com.vaadin.guice.annotation.AllKnownGuiceViews;
import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.DefaultUIProvider;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class VaadinModule extends AbstractModule {

    private final GuiceViewProvider viewProvider;
    private final GuiceUIProvider uiProvider;
    private final UIScoper uiScoper;
    private final Set<Class<? extends View>> views;

    public VaadinModule(SessionProvider sessionProvider, Set<Class<? extends View>> views, Set<Class<? extends UI>> uis, Set<Class<? extends ViewChangeListener>> viewChangeListeners, CurrentUIProvider currentUIProvider) {
        this.views = views;
        uiScoper = new UIScoper(sessionProvider, currentUIProvider);
        viewProvider = new GuiceViewProvider(views);
        uiProvider = new GuiceUIProvider(uis, viewChangeListeners, viewProvider, views, uiScoper);
    }

    @Override
    protected void configure() {
        bindScope(UIScope.class, uiScoper);
        bindScope(GuiceUI.class, uiScoper);
        bindScope(GuiceView.class, uiScoper);
        bind(UIProvider.class).toInstance(uiProvider);
        bind(ViewProvider.class).toInstance(viewProvider);
        final Multibinder<View> viewMultibinder = Multibinder.newSetBinder(binder(), View.class, AllKnownGuiceViews.class);

        for (Class<? extends View> guiceViewClass : views) {
            viewMultibinder.addBinding().to(guiceViewClass);
        }
    }

    public void vaadinInitialized(VaadinService service) {
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
        service.addSessionInitListener(uiProvider);
    }
}
