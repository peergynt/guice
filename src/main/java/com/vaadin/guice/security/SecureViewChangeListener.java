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

    @Inject
    @Named("guice_security_permission_denied_view")
    private Class<? extends View> permissionDeniedView;

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {

        if (!(event.getNewView() instanceof SecureView)) {
            return true;
        }

        boolean canAccess = ((SecureView) event.getNewView()).canAccess(event.getParameters());

        if (!canAccess) {
            String target = View.class.equals(permissionDeniedView)
                    ? ""
                    : permissionDeniedView.getAnnotation(GuiceView.class).value();

            UI.getCurrent().getNavigator().navigateTo(target);
        }

        return canAccess;
    }

    @Override
    public void afterViewChange(ViewChangeEvent event) {
    }
}
