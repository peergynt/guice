package com.vaadin.guice.server;

import com.google.common.base.Optional;

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
import static com.vaadin.guice.server.ReflectionUtils.getDefaultViewFieldAndNavigator;

final class NavigatorManager {

    private final Map<Class<? extends UI>, ViewFieldAndNavigator> uiToDefaultViewFieldAndNavigator = new ConcurrentHashMap<Class<? extends UI>, ViewFieldAndNavigator>();
    private final Optional<Class<? extends View>> errorViewClassOptional;
    private final GuiceVaadin guiceVaadin;

    NavigatorManager(GuiceVaadin guiceVaadin) {
        this.errorViewClassOptional = findErrorView(guiceVaadin.getViews());
        this.guiceVaadin = guiceVaadin;

        for (Class<? extends UI> knownUI : guiceVaadin.getUis()) {
            final Optional<ViewFieldAndNavigator> defaultViewFieldOptional = getDefaultViewFieldAndNavigator(knownUI);

            if (defaultViewFieldOptional.isPresent()) {
                uiToDefaultViewFieldAndNavigator.put(knownUI, defaultViewFieldOptional.get());
            }
        }
    }

    void addNavigator(UI ui) {

        final Class<? extends UI> uiClass = ui.getClass();

        ViewFieldAndNavigator viewFieldAndNavigator = uiToDefaultViewFieldAndNavigator.get(uiClass);

        if (viewFieldAndNavigator == null) {
            return;
        }

        Field defaultViewField = viewFieldAndNavigator.getViewField();
        Class<? extends GuiceNavigator> navigatorClass = viewFieldAndNavigator.getNavigator();

        Object defaultView;

        try {
            defaultView = defaultViewField.get(ui);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        checkNotNull(
                defaultView,
                "%s is annotated with @ViewContainer and therefore must not be null",
                defaultViewField.getName()
        );

        GuiceNavigator navigator = guiceVaadin.assemble(navigatorClass);

        if (defaultView instanceof ViewDisplay) {
            navigator.init(ui, (ViewDisplay) defaultView);
        } else if (defaultView instanceof ComponentContainer) {
            navigator.init(ui, (ComponentContainer) defaultView);
        } else if (defaultView instanceof SingleComponentContainer) {
            navigator.init(ui, (SingleComponentContainer) defaultView);
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
                            if (view == null) {
                                view = guiceVaadin.assemble(errorViewClassOptional.get());
                            }

                            return view;
                        }
                    }
            );
        }

        for (Class<? extends ViewChangeListener> viewChangeListenerClass : guiceVaadin.getViewChangeListeners(uiClass)) {
            ViewChangeListener viewChangeListener = guiceVaadin.assemble(viewChangeListenerClass);
            navigator.addViewChangeListener(viewChangeListener);
        }

        navigator.addProvider(guiceVaadin.getViewProvider());

        ui.setNavigator(navigator);
    }

    private boolean contains(Class<? extends UI>[] applicableUIs, Class<? extends UI> uiClass) {
        for (Class<? extends UI> applicableUiClass : applicableUIs) {
            if (uiClass.equals(applicableUiClass)) {
                return true;
            }
        }

        return false;
    }
}
