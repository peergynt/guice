package com.vaadin.guice.server;

import com.google.common.base.Optional;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.SingleComponentContainer;
import com.vaadin.ui.UI;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vaadin.guice.server.ReflectionUtils.findErrorView;
import static com.vaadin.guice.server.ReflectionUtils.getDefaultViewField;

final class NavigatorManager {

    private final Map<Class<? extends UI>, Field> uiToDefaultViewField = new ConcurrentHashMap<Class<? extends UI>, Field>();
    private final Optional<Class<? extends View>> errorView;
    private final GuiceVaadin guiceVaadin;
    private final Iterable<Class<? extends ViewChangeListener>> viewChangeListeners;
    private final ViewProvider viewProvider;

    NavigatorManager(Iterable<Class<? extends UI>> knownUIs, Iterable<Class<? extends View>> knownViews, Iterable<Class<? extends ViewChangeListener>> viewChangeListeners, ViewProvider viewProvider,
    GuiceVaadin guiceVaadin){
        this.viewChangeListeners = viewChangeListeners;
        this.viewProvider = viewProvider;

        this.errorView = findErrorView(knownViews);
        this.guiceVaadin = guiceVaadin;

        for (Class<? extends UI> knownUI : knownUIs) {
            final Optional<Field> defaultViewFieldOptional = getDefaultViewField(knownUI);

            if(defaultViewFieldOptional.isPresent()){
                uiToDefaultViewField.put(knownUI, defaultViewFieldOptional.get());
            }
        }
    }

    private Navigator createNavigator(UI instance, Object defaultView) {
        Navigator navigator;

        if (defaultView instanceof ComponentContainer) {
            navigator = new Navigator(instance, (ComponentContainer) defaultView);
        } else if (defaultView instanceof SingleComponentContainer) {
            navigator = new Navigator(instance, (SingleComponentContainer) defaultView);
        } else if (defaultView instanceof ViewDisplay) {
            navigator = new Navigator(instance, (ViewDisplay) defaultView);
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "%s is annotated with @ViewContainer, must be either ComponentContainer, SingleComponentContainer or ViewDisplay",
                            defaultView
                    )
            );
        }

        if (errorView.isPresent()) {
            navigator.setErrorProvider(
                    new ViewProvider() {
                        View instance;

                        @Override
                        public String getViewName(String viewAndParameters) {
                            return viewAndParameters;
                        }

                        @Override
                        public View getView(String viewName) {
                            if(instance == null){
                                instance = guiceVaadin.assemble(errorView.get());
                            }

                            return instance;
                        }
                    }
            );
        }

        for (Class<? extends ViewChangeListener> viewChangeListenerClass : viewChangeListeners) {
            ViewChangeListener viewChangeListener = guiceVaadin.assemble(viewChangeListenerClass);
            navigator.addViewChangeListener(viewChangeListener);
        }

        navigator.addProvider(viewProvider);

        return navigator;
    }

    void addNavigator(UI instance) {

        Field defaultViewField = uiToDefaultViewField.get(instance.getClass());

        if (defaultViewField != null) {

            Object defaultView;

            try {
                defaultView = defaultViewField.get(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            checkNotNull(
                    defaultView,
                    "%s is annotated with @ViewContainer and therefore must not be null",
                    defaultViewField.getName()
            );

            Navigator navigator = createNavigator(instance, defaultView);

            instance.setNavigator(navigator);
        }
    }
}
