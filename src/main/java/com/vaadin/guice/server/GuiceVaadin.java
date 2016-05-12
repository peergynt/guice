package com.vaadin.guice.server;

import com.google.inject.Injector;
import com.google.inject.Module;

import com.vaadin.guice.providers.CurrentUIProvider;
import com.vaadin.guice.providers.VaadinServiceProvider;
import com.vaadin.guice.providers.VaadinSessionProvider;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
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

import static com.google.inject.Guice.createInjector;
import static com.google.inject.util.Modules.combine;
import static com.google.inject.util.Modules.override;
import static com.vaadin.guice.server.ReflectionUtils.getDynamicModules;
import static com.vaadin.guice.server.ReflectionUtils.getGuiceUIClasses;
import static com.vaadin.guice.server.ReflectionUtils.getGuiceViewClasses;
import static com.vaadin.guice.server.ReflectionUtils.getStaticModules;
import static com.vaadin.guice.server.ReflectionUtils.getViewChangeListenerClasses;

/**
 * this class holds most of the logic that glues guice and vaadin together
 */
class GuiceVaadin implements SessionInitListener {

    private final GuiceViewProvider viewProvider;
    private final GuiceUIProvider guiceUIProvider;
    private final UIScoper uiScoper;
    private final VaadinSessionProvider vaadinSessionProvider;
    private final Set<Class<? extends UI>> uis;
    private final Set<Class<? extends View>> views;
    private final Set<Class<? extends ViewChangeListener>> viewChangeListeners;
    private final CurrentUIProvider currentUIProvider;
    private final VaadinServiceProvider vaadinServiceProvider;
    private final Injector injector;
    
    //used for non-testing
    GuiceVaadin(Reflections reflections, Class<? extends Module>[] modules){
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
                reflections,
                modules
        );
    }

    GuiceVaadin(
            VaadinSessionProvider vaadinSessionProvider,
            CurrentUIProvider currentUIProvider,
            VaadinServiceProvider vaadinServiceProvider,
            Reflections reflections,
            Class<? extends Module>[] modules
    ) {
        /**
         * combine bindings from the static modules in {@link Configuration#modules()} with those bindings
         * from dynamically loaded modules, see {@link com.vaadin.guice.annotation.UIModule}.
         * This is done first so modules can install their own reflections.
         */
        Module dynamicAndStaticModules = override(getStaticModules(modules, reflections)).with(getDynamicModules(reflections));

        Set<Class<? extends View>> views = getGuiceViewClasses(reflections);

        this.viewChangeListeners = getViewChangeListenerClasses(reflections);
        this.vaadinSessionProvider = vaadinSessionProvider;
        this.currentUIProvider = currentUIProvider;
        this.vaadinServiceProvider = vaadinServiceProvider;

        this.views = views;
        this.uis = getGuiceUIClasses(reflections);
        this.uiScoper = new UIScoper(vaadinSessionProvider, currentUIProvider);
        this.viewProvider = new GuiceViewProvider(views, this);
        this.guiceUIProvider = new GuiceUIProvider(this);

        //sets up the basic vaadin stuff like UIProvider
        VaadinModule vaadinModule = new VaadinModule(this);

        //combines static modules, dynamic modules and the VaadinModule
        Module combinedModule = combine(vaadinModule, dynamicAndStaticModules);

        this.injector = createInjector(combinedModule);
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

        //set the GuiceUIProvider
        event.getSession().addUIProvider(this.guiceUIProvider);
    }

    void vaadinInitialized() {
        VaadinService service = vaadinServiceProvider.get();

        //this glues guice to vaadin
        service.addSessionInitListener(this);

        service.addSessionDestroyListener(uiScoper);
        service.addSessionInitListener(uiScoper);
        service.addSessionDestroyListener(viewProvider);
        service.addSessionInitListener(viewProvider);
    }

    GuiceViewProvider getViewProvider() {
        return viewProvider;
    }

    GuiceUIProvider getGuiceUIProvider() {
        return guiceUIProvider;
    }

    UIScoper getUiScoper() {
        return uiScoper;
    }

    VaadinSessionProvider getVaadinSessionProvider() {
        return vaadinSessionProvider;
    }

    Set<Class<? extends View>> getViews() {
        return views;
    }

    CurrentUIProvider getCurrentUIProvider() {
        return currentUIProvider;
    }

    VaadinServiceProvider getVaadinServiceProvider() {
        return vaadinServiceProvider;
    }
    
    <T> T assemble(Class<T> type){
        return injector.getInstance(type);
    }

    Set<Class<? extends UI>> getUis() {
        return uis;
    }

    Set<Class<? extends ViewChangeListener>> getViewChangeListeners() {
        return viewChangeListeners;
    }

    Injector getInjector() {
        return injector;
    }
}
