package com.vaadin.guice.server;

import com.google.inject.Module;

import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.GuiceViewChangeListener;
import com.vaadin.guice.annotation.UIModule;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.UI;

import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

final class ReflectionUtils {

    private ReflectionUtils() {
    }

    static Set<Module> getUIModules(Reflections reflections) {
        Set<Module> dynamicallyLoadedModules = new HashSet<Module>();

        for (Class<?> dynamicallyLoadedModuleClass : reflections.getTypesAnnotatedWith(UIModule.class)) {
            checkArgument(
                    Module.class.isAssignableFrom(dynamicallyLoadedModuleClass),
                    "class %s is annotated with @UIModule but does not extend com.google.inject.Module",
                    dynamicallyLoadedModuleClass
            );

            try {
                dynamicallyLoadedModules.add(((Class<? extends Module>) dynamicallyLoadedModuleClass).newInstance());
            } catch (Exception e) {
                throw new RuntimeException("unable to instantiate " + dynamicallyLoadedModuleClass, e);
            }
        }
        return dynamicallyLoadedModules;
    }

    static Set<Class<? extends View>> getGuiceViewClasses(Reflections reflections) {
        Set<Class<? extends View>> views = new HashSet<Class<? extends View>>();

        for (Class<?> viewClass : reflections.getTypesAnnotatedWith(GuiceView.class)) {
            checkArgument(
                    View.class.isAssignableFrom(viewClass),
                    "class %s is annotated with @GuiceView but does not implement com.vaadin.navigator.View",
                    viewClass
            );

            views.add((Class<? extends View>) viewClass);
        }
        return views;
    }

    static Set<Class<? extends UI>> getGuiceUIClasses(Reflections reflections) {
        Set<Class<? extends UI>> uis = new HashSet<Class<? extends UI>>();

        for (Class<?> uiClass : reflections.getTypesAnnotatedWith(GuiceUI.class)) {
            checkArgument(
                    UI.class.isAssignableFrom(uiClass),
                    "class %s is annotated with @GuiceUI but does not extend com.vaadin.UI",
                    uiClass
            );

            uis.add((Class<? extends UI>) uiClass);
        }
        return uis;
    }

    static Set<Class<? extends ViewChangeListener>> getViewChangeListenerClasses(Reflections reflections) {
        Set<Class<? extends ViewChangeListener>> viewChangeListeners = new HashSet<Class<? extends ViewChangeListener>>();

        for (Class<?> viewChangeListenerClass : reflections.getTypesAnnotatedWith(GuiceViewChangeListener.class)) {
            checkArgument(
                    Module.class.isAssignableFrom(viewChangeListenerClass),
                    "class %s is annotated with @GuiceViewChangeListener but does not implement com.vaadin.navigator.ViewChangeListener",
                    viewChangeListenerClass
            );

            viewChangeListeners.add((Class<? extends ViewChangeListener>) viewChangeListenerClass);
        }
        return viewChangeListeners;
    }
}
