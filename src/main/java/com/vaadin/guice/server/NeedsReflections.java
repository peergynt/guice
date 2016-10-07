package com.vaadin.guice.server;

import com.google.inject.Injector;
import com.google.inject.Module;

import org.reflections.Reflections;

/**
 * This interface is to be applied to {@link Module} classes that are
 * loaded by a {@link GuiceVaadinServlet} and need an {@link Reflections}
 * instance
 */
public interface NeedsReflections extends Module {

    /**
     * this method will be called by guice-vaadin before 'configure' is called,
     * @param reflections the {@link Reflections} instance
     */
    void setReflections(Reflections reflections);
}
