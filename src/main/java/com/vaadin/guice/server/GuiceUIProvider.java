/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.guice.server;

import com.google.common.base.Optional;

import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.ViewContainer;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.SingleComponentContainer;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Vaadin {@link com.vaadin.server.UIProvider} that looks up UI classes from the Guice application
 * context. The UI classes must be annotated with {@link GuiceUI}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @author Bernd Hopp (bernd@vaadin.com)
 */
class GuiceUIProvider extends UIProvider implements SessionInitListener {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, Class<? extends UI>> pathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();
    private final Map<String, Class<? extends UI>> wildcardPathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();
    private final Map<Class<? extends UI>, Field> uiToDefaultViewField = new ConcurrentHashMap<Class<? extends UI>, Field>();
    private final Set<Class<? extends ViewChangeListener>> viewChangeListeners;
    private final GuiceViewProvider viewProvider;
    private final UIScoper uiScoper;
    private Optional<Class<? extends View>> errorView;

    @SuppressWarnings("unchecked")
    public GuiceUIProvider(Set<Class<? extends UI>> uiClasses, Set<Class<? extends ViewChangeListener>> viewChangeListeners, GuiceViewProvider viewProvider, Set<Class<? extends View>> viewClasses, UIScoper uiScoper) {
        this.viewProvider = viewProvider;
        this.uiScoper = uiScoper;
        detectUIs(uiClasses);

        errorView = findErrorView(viewClasses);

        if (pathToUIMap.isEmpty()) {
            logger.log(Level.WARNING, "Found no Vaadin UIs in the application context");
        }

        this.viewChangeListeners = viewChangeListeners;
    }

    private Optional<Class<? extends View>> findErrorView(Set<Class<? extends View>> viewClasses) {

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

    @SuppressWarnings("unchecked")
    private void detectUIs(Set<Class<? extends UI>> uiClasses) {
        logger.info("Checking the application context for Vaadin UIs");

        for (Class<? extends UI> uiClass : uiClasses) {

            logger.log(Level.INFO, "Found Vaadin UI [{0}]", uiClass.getCanonicalName());

            GuiceUI annotation = uiClass.getAnnotation(GuiceUI.class);

            checkState(annotation != null);

            String path = annotation.path();
            path = preparePath(path);

            Class<? extends UI> existingUiForPath = getUIByPath(path);

            checkState(
                    existingUiForPath == null,
                    "[%s] is already mapped to the path [%s]",
                    existingUiForPath,
                    path
            );

            logger.log(Level.INFO, "Mapping Vaadin UI [{0}] to path [{1}]",
                    new Object[]{uiClass.getCanonicalName(), path});
            mapPathToUI(path, uiClass);
            mapToDefaultViewField(uiClass);
        }
    }

    private void mapToDefaultViewField(Class<? extends UI> uiClass) {

        Field defaultViewField = null;

        for (java.lang.reflect.Field field : uiClass.getDeclaredFields()) {
            if (field.getAnnotation(ViewContainer.class) == null) {
                continue;
            }

            checkArgument(defaultViewField == null, "more than one field annotated with @ViewContainer in class " + uiClass);

            defaultViewField = field;
        }

        if (defaultViewField == null) {
            return;
        }

        defaultViewField.setAccessible(true);
        uiToDefaultViewField.put(uiClass, defaultViewField);
    }

    private String preparePath(String path) {
        if (path.length() > 0 && !path.startsWith("/")) {
            path = "/".concat(path);
        } else {
            // remove terminal slash from mapping
            path = path.replaceAll("/$", "");
        }

        return path;
    }

    @Override
    public Class<? extends UI> getUIClass(
            UIClassSelectionEvent uiClassSelectionEvent) {
        final String path = extractUIPathFromRequest(uiClassSelectionEvent
                .getRequest());
        if (pathToUIMap.containsKey(path)) {
            return pathToUIMap.get(path);
        }

        for (Map.Entry<String, Class<? extends UI>> entry : wildcardPathToUIMap
                .entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String extractUIPathFromRequest(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            String path = pathInfo;
            final int indexOfBang = path.indexOf('!');
            if (indexOfBang > -1) {
                path = path.substring(0, indexOfBang);
            }

            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return path;
        }
        return "";
    }

    private void mapPathToUI(String path, Class<? extends UI> uiClass) {
        if (path.endsWith("/*")) {
            wildcardPathToUIMap.put(path.substring(0, path.length() - 2),
                    uiClass);
        } else {
            pathToUIMap.put(path, uiClass);
        }
    }

    private Class<? extends UI> getUIByPath(String path) {
        return pathToUIMap.get(path);
    }

    @Override
    public UI createInstance(UICreateEvent event) {
        final Class<UIID> key = UIID.class;
        final UIID identifier = new UIID(event);
        CurrentInstance.set(key, identifier);

        try {
            uiScoper.startInitialization();

            UI instance = InjectorHolder.getInjector().getInstance(event.getUIClass());

            Field defaultViewField = uiToDefaultViewField.get(event.getUIClass());

            if (defaultViewField != null) {

                Object defaultView;

                try {
                    defaultView = defaultViewField.get(instance);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                checkNotNull(
                        defaultView,
                        "%s is annotated with @DefaultUI and therefore must not be null",
                        defaultViewField.getName()
                );

                Navigator navigator = createNavigator(instance, defaultView);

                navigator.addProvider(viewProvider);

                if (errorView.isPresent()) {
                    navigator.setErrorView(errorView.get());
                }

                for (Class<? extends ViewChangeListener> viewChangeListenerClass : viewChangeListeners) {
                    ViewChangeListener viewChangeListener = InjectorHolder.getInjector().getInstance(viewChangeListenerClass);
                    navigator.addViewChangeListener(viewChangeListener);
                }

                instance.setNavigator(navigator);
            }

            uiScoper.endInitialization(instance);

            return instance;
        } catch (RuntimeException e) {
            uiScoper.rollbackInitialization();
            throw e;
        } finally {
            CurrentInstance.set(key, null);
        }
    }

    Navigator createNavigator(UI instance, Object defaultView) {
        Navigator navigator;

        if (defaultView instanceof ComponentContainer) {
            navigator = new Navigator(instance, (ComponentContainer) defaultView);
        } else if (defaultView instanceof SingleComponentContainer) {
            navigator = new Navigator(instance, (SingleComponentContainer) defaultView);
        } else if (defaultView instanceof ViewDisplay) {
            navigator = new Navigator(instance, (ViewDisplay) defaultView);
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "%s is annotated with @DefaultUI, must be either ComponentContainer, SingleComponentContainer or ViewDisplay",
                            defaultView
                    )
            );
        }
        return navigator;
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        event.getSession().addUIProvider(this);
    }
}
