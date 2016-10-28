package com.vaadin.guice.security;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.vaadin.guice.annotation.GuiceViewChangeListener;

import org.vaadin.security.impl.SecureViewChangeListener;

@GuiceViewChangeListener
class GuiceSecureViewChangeListener extends SecureViewChangeListener {

    @Inject
    GuiceSecureViewChangeListener(@Named("guice_security_permission_denied_view") String permissionDeniedView) {
        super(permissionDeniedView);
    }
}
