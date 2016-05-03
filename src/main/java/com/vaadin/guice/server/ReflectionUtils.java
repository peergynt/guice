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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

final class ReflectionUtils {

    private ReflectionUtils() {
    }

    static Module create(Class<? extends Module> type, Reflections reflections) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        checkArgument(type.getConstructors().length == 1, type + " has more than 1 constructors");

        @SuppressWarnings("unchecked")
        Constructor<Module> constructor = (Constructor<Module>) type.getConstructors()[0];

        constructor.setAccessible(true);

        switch (constructor.getParameterTypes().length) {
            case 0:
                return constructor.newInstance();
            case 1:
                if (constructor.getParameterTypes()[0].equals(Reflections.class)) {
                    return constructor.newInstance(reflections);
                }
                break;
        }

        throw new IllegalArgumentException("no suitable constructor found for " + type);
    }

    @SuppressWarnings("unchecked")
    static Set<Module> getDynamicModules(Reflections reflections) {
        Set<Module> dynamicallyLoadedModules = new HashSet<Module>();

        for (Class<?> dynamicallyLoadedModuleClass : reflections.getTypesAnnotatedWith(UIModule.class, true)) {
            checkArgument(
                    Module.class.isAssignableFrom(dynamicallyLoadedModuleClass),
                    "class %s is annotated with @UIModule but does not implement com.google.inject.Module",
                    dynamicallyLoadedModuleClass
            );

            try {
                dynamicallyLoadedModules.add(create((Class<? extends Module>) dynamicallyLoadedModuleClass, reflections));
            } catch (Exception e) {
                throw new RuntimeException("unable to instantiate " + dynamicallyLoadedModuleClass, e);
            }
        }
        return dynamicallyLoadedModules;
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    static Set<Class<? extends ViewChangeListener>> getViewChangeListenerClasses(Reflections reflections) {
        Set<Class<? extends ViewChangeListener>> viewChangeListeners = new HashSet<Class<? extends ViewChangeListener>>();

        for (Class<?> viewChangeListenerClass : reflections.getTypesAnnotatedWith(GuiceViewChangeListener.class, true)) {
            checkArgument(
                    ViewChangeListener.class.isAssignableFrom(viewChangeListenerClass),
                    "class %s is annotated with @GuiceViewChangeListener but does not implement com.vaadin.navigator.ViewChangeListener",
                    viewChangeListenerClass
            );

            viewChangeListeners.add((Class<? extends ViewChangeListener>) viewChangeListenerClass);
        }
        return viewChangeListeners;
    }
}
