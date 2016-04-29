package com.vaadin.guice.server;

import com.google.common.base.Strings;

import com.vaadin.guice.access.ViewAccessControl;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.UI;

import static com.google.common.base.Strings.isNullOrEmpty;

class ViewAccessControlChangeListener implements ViewChangeListener{

    private final ViewAccessControl viewInstanceAccessControl;
    private String accessDeniedTarget;

    ViewAccessControlChangeListener(ViewAccessControl viewInstanceAccessControl){
        this.viewInstanceAccessControl = viewInstanceAccessControl;
    }

    @Override
    public boolean beforeViewChange(ViewChangeEvent viewChangeEvent) {
        boolean accessGranted = viewInstanceAccessControl.isAccessGranted(UI.getCurrent(), viewChangeEvent.getViewName());

        if(!accessGranted && !isNullOrEmpty(accessDeniedTarget)){
            viewChangeEvent.getNavigator().navigateTo(accessDeniedTarget);
        }

        return accessGranted;
    }

    @Override
    public void afterViewChange(ViewChangeEvent viewChangeEvent) {
        ;
    }

    public void setAccessDeniedTarget(String accessDeniedTarget) {
        this.accessDeniedTarget = accessDeniedTarget;
    }
}
