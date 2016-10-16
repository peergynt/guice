package com.vaadin.guice.security;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.NeedsPermission;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.navigator.Navigator;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import java.util.Set;

@UIScope
class PermissionEnforcerImpl implements PermissionEnforcer {

    @Inject
    private PermissionEvaluator permissionEvaluator;

    @Inject
    @AllRestrictedComponents
    private Set<Component> restrictedComponents;

    @Override
    public void enforce() {

        UI ui = UI.getCurrent();

        if (ui != null) {
            final Navigator navigator = ui.getNavigator();

            if (navigator != null && navigator.getCurrentView() != null && navigator.getCurrentView() instanceof SecureView) {
                //reload secure view to check if there is still access granted
                navigator.navigateTo(navigator.getState());
            }
        }

        for (Component restrictedComponent : restrictedComponents) {
            Class<? extends Component> restrictedComponentClass = restrictedComponent.getClass();

            NeedsPermission needsPermission = restrictedComponentClass.getAnnotation(NeedsPermission.class);

            boolean hasPermission = permissionEvaluator.hasPermission(needsPermission.value());

            restrictedComponent.setVisible(hasPermission);
        }
    }
}
