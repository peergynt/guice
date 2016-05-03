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

final class NavigatorUtil {

    private NavigatorUtil(){
    }

    static Navigator createNavigator(UI instance, Object defaultView, final Optional<Class<? extends View>> errorView,
                                     Iterable<Class<? extends ViewChangeListener>> viewChangeListeners) {
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
                                instance = InjectorHolder.getInjector().getInstance(errorView.get());
                            }

                            return instance;
                        }
                    }
            );
        }

        for (Class<? extends ViewChangeListener> viewChangeListenerClass : viewChangeListeners) {
            ViewChangeListener viewChangeListener = InjectorHolder.getInjector().getInstance(viewChangeListenerClass);
            navigator.addViewChangeListener(viewChangeListener);
        }

        return navigator;
    }
}
