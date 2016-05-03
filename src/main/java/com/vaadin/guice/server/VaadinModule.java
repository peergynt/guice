package com.vaadin.guice.server;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import com.vaadin.guice.annotation.AllKnownGuiceViews;
import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.guice.providers.CurrentUIProvider;
import com.vaadin.guice.providers.VaadinSessionProvider;
import com.vaadin.guice.providers.VaadinServiceProvider;
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

import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.vaadin.guice.server.ReflectionUtils.getGuiceUIClasses;
import static com.vaadin.guice.server.ReflectionUtils.getGuiceViewClasses;
import static com.vaadin.guice.server.ReflectionUtils.getViewChangeListenerClasses;

class VaadinModule extends AbstractModule implements SessionInitListener{

    private final GuiceViewProvider viewProvider;
    private final GuiceUIProvider uiProvider;
    private final UIScoper uiScoper;
    private final VaadinSessionProvider vaadinSessionProvider;
    private final Set<Class<? extends View>> views;
    private final CurrentUIProvider currentUIProvider;
    private final VaadinServiceProvider vaadinServiceProvider;

    //used for non-testing
    public VaadinModule(Reflections reflections){
        this(
                new VaadinSessionProvider() {
                    @Override
                    public VaadinSession get() {
                        return VaadinSession.getCurrent();
                    }
                },
                new CurrentUIProvider() {
                    @Override
                    public UI get() {
                        return UI.getCurrent();
                    }
                },
                new VaadinServiceProvider() {
                    @Override
                    public VaadinService get() {
                        return VaadinService.getCurrent();
                    }
                },
                reflections
        );
    }

    public VaadinModule(
            VaadinSessionProvider vaadinSessionProvider,
            CurrentUIProvider currentUIProvider,
            VaadinServiceProvider vaadinServiceProvider,
            Reflections reflections
    ) {
        Set<Class<? extends UI>> uis = getGuiceUIClasses(reflections);

        Set<Class<? extends View>> views = getGuiceViewClasses(reflections);

        Set<Class<? extends ViewChangeListener>> viewChangeListeners = getViewChangeListenerClasses(reflections);

        this.vaadinSessionProvider = vaadinSessionProvider;
        this.currentUIProvider = currentUIProvider;
        this.vaadinServiceProvider = vaadinServiceProvider;

        this.views = views;
        this.uiScoper = new UIScoper(vaadinSessionProvider, currentUIProvider);
        this.viewProvider = new GuiceViewProvider(views);
        this.uiProvider = new GuiceUIProvider(uis, viewChangeListeners, viewProvider, views, uiScoper);
    }

    @Override
    protected void configure() {
        bindScope(UIScope.class, uiScoper);
        bindScope(GuiceUI.class, uiScoper);
        bindScope(GuiceView.class, uiScoper);
        bind(UIProvider.class).toInstance(uiProvider);
        bind(ViewProvider.class).toInstance(viewProvider);

        bind(VaadinSession.class).toProvider(vaadinSessionProvider);
        bind(UI.class).toProvider(currentUIProvider);
        bind(VaadinService.class).toProvider(vaadinServiceProvider);

        bind(VaadinServiceProvider.class).toInstance(vaadinServiceProvider);
        bind(CurrentUIProvider.class).toInstance(currentUIProvider);
        bind(VaadinSessionProvider.class).toInstance(vaadinSessionProvider);

        final Multibinder<View> viewMultibinder = Multibinder.newSetBinder(binder(), View.class, AllKnownGuiceViews.class);

        for (Class<? extends View> guiceViewClass : views) {
            viewMultibinder.addBinding().to(guiceViewClass);
        }
    }

    public void vaadinInitialized() {
        VaadinService service = vaadinServiceProvider.get();

        service.addSessionInitListener(this);
        service.addSessionDestroyListener(uiScoper);
        service.addSessionInitListener(uiScoper);
        service.addSessionDestroyListener(viewProvider);
        service.addSessionInitListener(viewProvider);
        service.addSessionInitListener(uiProvider);
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        // remove DefaultUIProvider instances to avoid mapping
        // extraneous UIs if e.g. a servlet is declared as a nested
        // class in a UI class
        VaadinSession session = event.getSession();
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
}
