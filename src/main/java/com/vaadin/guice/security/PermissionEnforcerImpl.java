package com.vaadin.guice.security;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.vaadin.guice.annotation.GuardedBy;
import com.vaadin.guice.annotation.GuiceViewChangeListener;
import com.vaadin.guice.annotation.NeedsPermission;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import java.util.Set;

@GuiceViewChangeListener
@UIScope
class PermissionEnforcerImpl implements ViewChangeListener, PermissionEnforcer {

    @Inject
    private PermissionEvaluator permissionEvaluator;

    @Inject
    @Named("guice_security_permission_denied_view")
    private String permissionDeniedView;

    @Inject
    @AllGuards
    private Set<Guard> allGuards;

    @Inject
    @AllRestrictedComponents
    private Set<Component> restrictedComponents;

    public boolean beforeViewChange(ViewChangeEvent viewChangeEvent) {

        final Class<? extends View> newViewClass = viewChangeEvent.getNewView().getClass();

        boolean isPermitted = isPermitted(newViewClass);

        if (!isPermitted) {
            navigateToPermissionDeniedView();
        }

        return isPermitted;
    }

    public void afterViewChange(ViewChangeEvent viewChangeEvent) {
    }


    private boolean isPermitted(Class<?> clazz) {
        NeedsPermission needsPermission = clazz.getAnnotation(NeedsPermission.class);

        if (needsPermission != null) {
            if (!permissionEvaluator.hasPermission(needsPermission.value())) {
                return false;
            }
        }

        GuardedBy guardedBy = clazz.getAnnotation(GuardedBy.class);

        if (guardedBy != null) {
            for (Class<? extends Guard> guardClass : guardedBy.value()) {
                final Guard guard = getGuard(guardClass);

                if (!guard.hasAccess()) {
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private <T extends Guard> T getGuard(Class<T> guardClass) {
        for (Guard guard : allGuards) {
            if (guardClass.equals(guard.getClass())) {
                return (T) guard;
            }
        }

        throw new IllegalStateException("no guard of type " + guardClass + "bound");
    }

    private void navigateToPermissionDeniedView() {
        UI.getCurrent().getNavigator().navigateTo(permissionDeniedView);
    }

    @Override
    public void enforce() {
        final View currentView = UI.getCurrent().getNavigator().getCurrentView();

        if (currentView != null) {
            final Class<? extends View> viewClass = currentView.getClass();

            if (!isPermitted(viewClass)) {
                navigateToPermissionDeniedView();
            }
        }

        for (Component restrictedComponent : restrictedComponents) {
            Class<? extends Component> restrictedComponentClass = restrictedComponent.getClass();

            restrictedComponent.setVisible(isPermitted(restrictedComponentClass));
        }
    }
}
