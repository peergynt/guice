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
    private final Optional<Class<? extends View>> errorViewClassOptional;
    private final GuiceVaadin guiceVaadin;

    NavigatorManager(GuiceVaadin guiceVaadin){

        this.errorViewClassOptional = findErrorView(guiceVaadin.getViews());
        this.guiceVaadin = guiceVaadin;

        for (Class<? extends UI> knownUI : guiceVaadin.getUis()) {
            final Optional<Field> defaultViewFieldOptional = getDefaultViewField(knownUI);

            if(defaultViewFieldOptional.isPresent()){
                uiToDefaultViewField.put(knownUI, defaultViewFieldOptional.get());
            }
        }
    }

    void addNavigator(UI instance) {

        Field defaultViewField = uiToDefaultViewField.get(instance.getClass());

        if (defaultViewField == null) {
            return;
        }

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

        if (errorViewClassOptional.isPresent()) {
            navigator.setErrorProvider(
                    new ViewProvider() {
                        View view;

                        @Override
                        public String getViewName(String viewAndParameters) {
                            return viewAndParameters;
                        }

                        @Override
                        public View getView(String viewName) {
                            if(view == null){
                                view = guiceVaadin.assemble(errorViewClassOptional.get());
                            }

                            return view;
                        }
                    }
            );
        }

        for (Class<? extends ViewChangeListener> viewChangeListenerClass : guiceVaadin.getViewChangeListeners()) {
            ViewChangeListener viewChangeListener = guiceVaadin.assemble(viewChangeListenerClass);
            navigator.addViewChangeListener(viewChangeListener);
        }

        navigator.addProvider(guiceVaadin.getViewProvider());

        instance.setNavigator(navigator);
    }
}
