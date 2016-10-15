package com.vaadin.guice.server;

import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.UIScope;
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
import static java.lang.String.format;

final class NavigatorManager {

    private final GuiceVaadin guiceVaadin;

    NavigatorManager(GuiceVaadin guiceVaadin) {
        this.guiceVaadin = guiceVaadin;
    }

    void addNavigator(UI ui) {

        final Class<? extends UI> uiClass = ui.getClass();

        final GuiceUI annotation = uiClass.getAnnotation(GuiceUI.class);

        checkState(annotation != null);

        if (annotation.viewContainer().equals(Component.class)) {
            return;
        }

        Class<? extends Component> viewContainerClass = annotation.viewContainer();

        checkState(
                viewContainerClass.getAnnotation(UIScope.class) != null,
                "%s is annotated with having %s as it's viewContainer, but this class does not have a @UIScope annotation. " +
                        "ViewContainers must be put in UIScope",
                uiClass, viewContainerClass
        );

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

        if (!View.class.equals(annotation.errorView())) {

            navigator.setErrorProvider(
                    new ViewProvider() {
                        @Override
                        public String getViewName(String viewAndParameters) {
                            return removeParametersFromViewName(viewAndParameters);
                        }

                        @Override
                        public View getView(String viewName) {
                            //noinspection OptionalGetWithoutIsPresent
                            return guiceVaadin.assemble(annotation.errorView());
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
