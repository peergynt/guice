package com.vaadin.guice.security;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.navigator.Navigator;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import org.vaadin.security.annotation.Restricted;
import org.vaadin.security.api.PermissionEnforcer;
import org.vaadin.security.api.PermissionEvaluator;
import org.vaadin.security.api.SecureView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@UIScope
class GuicePermissionEnforcer implements PermissionEnforcer {

    private final Map<String, Component> restrictedComponentsByPermission;
    @Inject
    private PermissionEvaluator permissionEvaluator;

    @Inject
    GuicePermissionEnforcer(@AllRestrictedComponents Set<Component> restrictedComponents) {
        restrictedComponentsByPermission = new HashMap<String, Component>(restrictedComponents.size());

        for (Component component : restrictedComponents) {
            restrictedComponentsByPermission.put(
                    component.getClass().getAnnotation(Restricted.class).value(),
                    component
            );
        }
    }

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

        for (Map.Entry<String, Component> entry : restrictedComponentsByPermission.entrySet()) {
            boolean hasPermission = permissionEvaluator.hasPermission(entry.getKey());

            entry.getValue().setVisible(hasPermission);
        }
    }


    @Override
    public void register(Component component, String permission) {
        checkNotNull(component);
        checkArgument(!isNullOrEmpty(permission));

        restrictedComponentsByPermission.put(permission, component);
    }
}
