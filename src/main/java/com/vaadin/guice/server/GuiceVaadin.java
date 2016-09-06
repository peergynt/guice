package com.vaadin.guice.server;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

import com.vaadin.navigator.Navigator;
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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
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
    private final Provider<VaadinSession> vaadinSessionProvider;
    private final Set<Class<? extends UI>> uis;
    private final Set<Class<? extends View>> views;
    private final Map<Class<? extends UI>, Set<Class<? extends ViewChangeListener>>> viewChangeListeners;
    private final Provider<UI> currentUIProvider;
    private final Provider<VaadinService> vaadinServiceProvider;
    private final Injector injector;
    private final VaadinSessionScoper vaadinSessionScoper;
    private final ViewScoper viewScoper;

    //used for non-testing
    GuiceVaadin(Reflections reflections, Class<? extends Module>[] modules) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        this(
                new Provider<VaadinSession>() {
                    @Override
                    public VaadinSession get() {
                        return VaadinSession.getCurrent();
                    }
                },
                new Provider<UI>() {
                    @Override
                    public UI get() {
                        return UI.getCurrent();
                    }
                },
                new Provider<View>() {
                    @Override
                    public View get() {
                        final Navigator navigator = UI.getCurrent().getNavigator();

                        checkState(navigator != null);

                        return navigator.getCurrentView();
                    }
                },
                new Provider<VaadinService>() {
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
            Provider<VaadinSession> vaadinSessionProvider,
            Provider<UI> currentUIProvider,
            Provider<View> currentViewProvider,
            Provider<VaadinService> vaadinServiceProvider,
            Reflections reflections,
            Class<? extends Module>[] modules
    ) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        /*
         * combine bindings from the static modules in {@link Configuration#modules()} with those bindings
         * from dynamically loaded modules, see {@link com.vaadin.guice.annotation.UIModule}.
         * This is done first so modules can install their own reflections.
         */
        Module dynamicAndStaticModules = override(getStaticModules(modules, reflections, this)).with(getDynamicModules(reflections, this));

        Set<Class<? extends View>> views = getGuiceViewClasses(reflections);

        this.uis = getGuiceUIClasses(reflections);
        this.viewChangeListeners = getViewChangeListenerClasses(reflections, uis);
        this.vaadinSessionProvider = vaadinSessionProvider;
        this.currentUIProvider = currentUIProvider;
        this.vaadinServiceProvider = vaadinServiceProvider;

        this.views = views;

        this.uiScoper = new UIScoper(vaadinSessionProvider, currentUIProvider);
        this.viewScoper = new ViewScoper(vaadinSessionProvider, currentViewProvider);
        this.vaadinSessionScoper = new VaadinSessionScoper(vaadinSessionProvider);
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

        final String DefaultUiProviderCanonicalName = DefaultUIProvider.class.getCanonicalName();

        for (UIProvider uiProvider : session.getUIProviders()) {
            // use canonical names as these may have been loaded with
            // different classloaders
            if (DefaultUiProviderCanonicalName.equals(uiProvider.getClass().getCanonicalName())) {
                session.removeUIProvider(uiProvider);
            }
        }

        //set the GuiceUIProvider
        session.addUIProvider(this.guiceUIProvider);
    }

    void vaadinInitialized() {
        VaadinService service = vaadinServiceProvider.get();

        //this glues guice to vaadin
        service.addSessionInitListener(this);

        service.addSessionDestroyListener(uiScoper);
        service.addSessionInitListener(uiScoper);
        service.addSessionDestroyListener(viewScoper);
        service.addSessionInitListener(viewScoper);
        service.addSessionDestroyListener(viewProvider);
        service.addSessionInitListener(viewProvider);
        service.addSessionDestroyListener(vaadinSessionScoper);
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

    Provider<VaadinSession> getVaadinSessionProvider() {
        return vaadinSessionProvider;
    }

    Set<Class<? extends View>> getViews() {
        return views;
    }

    Provider<UI> getCurrentUIProvider() {
        return currentUIProvider;
    }

    Provider<VaadinService> getVaadinServiceProvider() {
        return vaadinServiceProvider;
    }

    <T> T assemble(Class<T> type) {
        return injector.getInstance(type);
    }

    Set<Class<? extends UI>> getUis() {
        return uis;
    }

    Set<Class<? extends ViewChangeListener>> getViewChangeListeners(Class<? extends UI> uiClass) {
        return viewChangeListeners.get(uiClass);
    }

    Injector getInjector() {
        return injector;
    }

    VaadinSessionScoper getVaadinSessionScoper() {
        return vaadinSessionScoper;
    }

    ViewScoper getViewScoper() {
        return viewScoper;
    }

}
