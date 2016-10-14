package com.vaadin.guice.security;

import com.vaadin.navigator.View;

public interface SecureView extends View {
    boolean canAccess(String parameters);
}
