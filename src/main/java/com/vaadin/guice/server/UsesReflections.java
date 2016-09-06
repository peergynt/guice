package com.vaadin.guice.server;

import com.google.inject.Module;

import org.reflections.Reflections;

public interface UsesReflections extends Module {
    void setReflections(Reflections reflections);
}
