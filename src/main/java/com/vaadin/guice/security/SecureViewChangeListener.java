package com.vaadin.guice.security;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.GuiceViewChangeListener;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.UI;

@GuiceViewChangeListener
@UIScope
class SecureViewChangeListener implements ViewChangeListener {

    private final String permissionDeniedTarget;

    @Inject
    SecureViewChangeListener(@Named("guice_security_permission_denied_view") Class<? extends View> permissionDeniedView) {
        permissionDeniedTarget = View.class.equals(permissionDeniedView)
                ? ""
                : permissionDeniedView.getAnnotation(GuiceView.class).value();
    }

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {

        if (!(event.getNewView() instanceof SecureView)) {
            return true;
        }

        boolean canAccess = ((SecureView) event.getNewView()).canAccess(event.getParameters());

        if (!canAccess) {
            UI.getCurrent().getNavigator().navigateTo(permissionDeniedTarget);
        }

        return canAccess;
    }

    @Override
    public void afterViewChange(ViewChangeEvent event) {
    }
}
