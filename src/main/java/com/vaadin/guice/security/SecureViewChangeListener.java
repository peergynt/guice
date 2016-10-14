package com.vaadin.guice.security;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.vaadin.guice.annotation.GuiceViewChangeListener;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.UI;

@GuiceViewChangeListener
@UIScope
class SecureViewChangeListener implements ViewChangeListener {

    @Inject
    @Named("guice_security_permission_denied_view")
    private String permissionDeniedView;

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {

        if (!(event.getNewView() instanceof SecureView)) {
            return true;
        }

        boolean canAccess = ((SecureView) event.getNewView()).canAccess(event.getParameters());

        if (!canAccess) {
            UI.getCurrent().getNavigator().navigateTo(permissionDeniedView);
        }

        return canAccess;
    }

    @Override
    public void afterViewChange(ViewChangeEvent event) {
    }
}
