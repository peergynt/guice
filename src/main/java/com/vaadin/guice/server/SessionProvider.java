package com.vaadin.guice.server;

import com.vaadin.server.VaadinSession;

interface SessionProvider {
    VaadinSession getCurrentSession();
}
