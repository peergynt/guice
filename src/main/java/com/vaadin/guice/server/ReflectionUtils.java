package com.vaadin.guice.server;

import com.google.common.base.Optional;
import com.google.inject.Module;

import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.GuiceViewChangeListener;
import com.vaadin.guice.annotation.UIModule;
import com.vaadin.guice.annotation.ViewContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.UI;

import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.vaadin.guice.server.PathUtil.preparePath;

final class ReflectionUtils {

    private ReflectionUtils() {
    }

    private static Module create(Class<? extends Module> type, Reflections reflections) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        checkArgument(type.getDeclaredConstructors().length == 1, type + " has more than 1 constructors");

        @SuppressWarnings("unchecked")
        Constructor<Module> constructor = (Constructor<Module>) type.getDeclaredConstructors()[0];

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

    static List<Module> getStaticModules(Class<? extends Module>[] modules, Reflections reflections) {
        List<Module> hardWiredModules = new ArrayList<Module>(modules.length);

        for (Class<? extends Module> moduleClass : modules) {
            try {
                hardWiredModules.add(create(moduleClass, reflections));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return hardWiredModules;
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

    @SuppressWarnings("unchecked")
    static Optional<ViewFieldAndNavigator> getDefaultViewFieldAndNavigator(Class<? extends UI> uiClass) {

        Field defaultViewField = null;
        Class<? extends GuiceNavigator> navigatorClass = null;

        while ((uiClass != null) && (uiClass != UI.class)) {
            for (Field field : uiClass.getDeclaredFields()) {

                final ViewContainer viewContainer = field.getAnnotation(ViewContainer.class);

                if (viewContainer == null) {
                    continue;
                }

                checkArgument(defaultViewField == null, "more than one field annotated with @ViewContainer in class " + uiClass);

                defaultViewField = field;
                navigatorClass = viewContainer.navigator();
            }
            uiClass = (Class<? extends UI>) uiClass.getSuperclass();
        }

        if (defaultViewField == null) {
            return Optional.absent();
        }

        defaultViewField.setAccessible(true);
        return Optional.of(new ViewFieldAndNavigator(defaultViewField, navigatorClass));
    }

    static Optional<Class<? extends View>> findErrorView(Iterable<Class<? extends View>> viewClasses) {

        Class<? extends View> errorView = null;

        for (Class<? extends View> viewClass : viewClasses) {
            GuiceView annotation = viewClass.getAnnotation(GuiceView.class);

            checkState(annotation != null);

            if (annotation.isErrorView()) {
                checkState(
                        errorView == null,
                        "%s and %s have an @GuiceView-annotation with isErrorView set to true",
                        errorView,
                        viewClass
                );

                errorView = viewClass;
            }
        }

        return Optional.<Class<? extends View>>fromNullable(errorView);
    }

    static void detectUIs(Set<Class<? extends UI>> uiClasses, Map<String, Class<? extends UI>> pathToUIMap, Map<String, Class<? extends UI>> wildcardPathToUIMap) {
        Logger logger = Logger.getLogger(ReflectionUtils.class.getName());

        logger.info("Checking the application context for Vaadin UIs");

        for (Class<? extends UI> uiClass : uiClasses) {

            logger.log(Level.INFO, "Found Vaadin UI [{0}]", uiClass.getCanonicalName());

            GuiceUI annotation = uiClass.getAnnotation(GuiceUI.class);

            checkState(annotation != null);

            String path = annotation.path();
            path = preparePath(path);

            Class<? extends UI> existingUiForPath = pathToUIMap.get(path);

            checkState(
                    existingUiForPath == null,
                    "[%s] is already mapped to the path [%s]",
                    existingUiForPath,
                    path
            );

            logger.log(Level.INFO, "Mapping Vaadin UI [{0}] to path [{1}]",
                    new Object[]{uiClass.getCanonicalName(), path});

            if (path.endsWith("/*")) {
                wildcardPathToUIMap.put(path.substring(0, path.length() - 2),
                        uiClass);
            } else {
                pathToUIMap.put(path, uiClass);
            }
        }

        if (pathToUIMap.isEmpty()) {
            logger.log(Level.WARNING, "Found no Vaadin UIs in the application context");
        }
    }
}

