package com.vaadin.guice.server;

import com.vaadin.guice.access.ViewInstanceAccessControl;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.UI;

class ViewInstanceAccessControlChangeListener implements ViewChangeListener{

    private final ViewInstanceAccessControl viewInstanceAccessControl;

    public ViewInstanceAccessControlChangeListener(ViewInstanceAccessControl viewInstanceAccessControl){
        this.viewInstanceAccessControl = viewInstanceAccessControl;
    }

    @Override
    public boolean beforeViewChange(ViewChangeEvent viewChangeEvent) {
        return viewInstanceAccessControl.isAccessGranted(UI.getCurrent(), viewChangeEvent.getViewName(), viewChangeEvent.getNewView());
    }

    @Override
    public void afterViewChange(ViewChangeEvent viewChangeEvent) {
        ;
    }
}
