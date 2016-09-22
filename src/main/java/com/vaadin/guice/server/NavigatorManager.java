package com.vaadin.guice.server;

import com.google.common.base.Optional;

import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.SingleComponentContainer;
import com.vaadin.ui.UI;

import static com.google.common.base.Preconditions.checkState;
import static com.vaadin.guice.server.PathUtil.removeParametersFromViewName;
import static com.vaadin.guice.server.ReflectionUtils.findErrorView;
import static java.lang.String.format;

final class NavigatorManager {

    private final Optional<Class<? extends View>> errorViewClassOptional;
    private final GuiceVaadin guiceVaadin;

    NavigatorManager(GuiceVaadin guiceVaadin) {
        this.errorViewClassOptional = findErrorView(guiceVaadin.getViews());
        this.guiceVaadin = guiceVaadin;
    }

    void addNavigator(UI ui) {

        final Class<? extends UI> uiClass = ui.getClass();

        GuiceUI annotation = uiClass.getAnnotation(GuiceUI.class);

        checkState(annotation != null);

        if (annotation.viewContainer().equals(Component.class)) {
            return;
        }

        Class<? extends Component> viewContainerClass = annotation.viewContainer();
        Class<? extends GuiceNavigator> navigatorClass = annotation.navigator();

        Component defaultView = guiceVaadin.assemble(viewContainerClass);

        GuiceNavigator navigator = guiceVaadin.assemble(navigatorClass);

        if (defaultView instanceof ViewDisplay) {
            navigator.init(ui, (ViewDisplay) defaultView);
        } else if (defaultView instanceof ComponentContainer) {
            navigator.init(ui, (ComponentContainer) defaultView);
        } else if (defaultView instanceof SingleComponentContainer) {
            navigator.init(ui, (SingleComponentContainer) defaultView);
        } else {
            throw new IllegalArgumentException(
                    format(
                            "%s is set as viewContainer() in @GuiceUI of %s, must be either ComponentContainer, SingleComponentContainer or ViewDisplay",
                            viewContainerClass,
                            uiClass
                    )
            );
        }

        if (errorViewClassOptional.isPresent()) {
            navigator.setErrorProvider(
                    new ViewProvider() {
                        @Override
                        public String getViewName(String viewAndParameters) {
                            return removeParametersFromViewName(viewAndParameters);
                        }

                        @Override
                        public View getView(String viewName) {
                            //noinspection OptionalGetWithoutIsPresent
                            return guiceVaadin.assemble(errorViewClassOptional.get());
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
}
