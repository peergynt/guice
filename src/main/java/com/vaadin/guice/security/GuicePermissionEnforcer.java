package com.vaadin.guice.security;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.navigator.Navigator;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import org.vaadin.security.annotation.Restricted;
import org.vaadin.security.api.PermissionEnforcer;
import org.vaadin.security.api.PermissionEvaluator;
import org.vaadin.security.api.SecureView;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@UIScope
class GuicePermissionEnforcer implements PermissionEnforcer {

    private final Multimap<String, Component> restrictedComponentsByPermission;

    @Inject
    private PermissionEvaluator permissionEvaluator;

    @Inject
    GuicePermissionEnforcer(@AllRestrictedComponents Set<Component> restrictedComponents) {
        restrictedComponentsByPermission = HashMultimap.create(restrictedComponents.size(), 4);

        for (Component component : restrictedComponents) {
            final String restriction = component.getClass().getAnnotation(Restricted.class).value();

            restrictedComponentsByPermission.put(
                    restriction,
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

        for (Map.Entry<String, Collection<Component>> entry : restrictedComponentsByPermission.asMap().entrySet()) {
            String permission = entry.getKey();
            Collection<Component> components = entry.getValue();

            boolean hasPermission = permissionEvaluator.hasPermission(permission);

            for (Component component : components) {
                component.setVisible(hasPermission);
            }
        }
    }


    @Override
    public void register(Component component, String permission) {
        checkNotNull(component);
        checkArgument(!isNullOrEmpty(permission));

        restrictedComponentsByPermission.put(permission, component);
    }
}
