package com.vaadin.guice.server;

import com.vaadin.guice.access.ViewInstanceAccessControl;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.UI;

import static com.google.common.base.Strings.isNullOrEmpty;

class ViewInstanceAccessControlChangeListener implements ViewChangeListener {

    private final ViewInstanceAccessControl viewInstanceAccessControl;
    private final String accessDeniedTarget;

    public ViewInstanceAccessControlChangeListener(ViewInstanceAccessControl viewInstanceAccessControl, String accessDeniedTarget) {
        this.viewInstanceAccessControl = viewInstanceAccessControl;
        this.accessDeniedTarget = accessDeniedTarget;
    }

    @Override
    public boolean beforeViewChange(ViewChangeEvent viewChangeEvent) {
        boolean accessGranted = viewInstanceAccessControl.isAccessGranted(UI.getCurrent(), viewChangeEvent.getViewName(), viewChangeEvent.getNewView());

        if (!accessGranted && !isNullOrEmpty(accessDeniedTarget)) {
            viewChangeEvent.getNavigator().navigateTo(accessDeniedTarget);
        }

        return accessGranted;
    }

    @Override
    public void afterViewChange(ViewChangeEvent viewChangeEvent) {
        ;
    }
}