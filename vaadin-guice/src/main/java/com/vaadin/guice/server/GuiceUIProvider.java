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

import com.vaadin.guice.annotation.DefaultView;
import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.navigator.ViewProvider;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Vaadin {@link com.vaadin.server.UIProvider} that looks up UI classes from the Guice application
 * context. The UI classes must be annotated with {@link GuiceUI}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @author Bernd Hopp (bernd@vaadin.com)
 */
class GuiceUIProvider extends UIProvider implements SessionInitListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, Class<? extends UI>> pathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();
    private final Map<String, Class<? extends UI>> wildcardPathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();
    private final Map<Class<? extends UI>, Field> uiToDefaultViewField = new ConcurrentHashMap<Class<? extends UI>, Field>();

    public GuiceUIProvider(Set<Class<?>> uiClasses) {
        detectUIs(uiClasses);

        if (pathToUIMap.isEmpty()) {
            logger.warn("Found no Vaadin UIs in the application context");
        }
    }

    @SuppressWarnings("unchecked")
    private void detectUIs(Set<Class<?>> uiClasses) {
        logger.info("Checking the application context for Vaadin UIs");

        for (Class<?> uiClassRaw : uiClasses) {
            checkArgument(UI.class.isAssignableFrom(uiClassRaw), "class %s has GuiceUI annotation but is not of type com.vaadin.ui.UI.", uiClassRaw);

            Class<? extends UI> uiClass = (Class<? extends UI>) uiClassRaw;

            logger.info("Found Vaadin UI [{}]", uiClass.getCanonicalName());

            GuiceUI annotation = uiClass.getAnnotation(GuiceUI.class);

            String path = annotation.path();
            path = preparePath(path);

            Class<? extends UI> existingUiForPath = getUIByPath(path);
            if (existingUiForPath != null) {
                throw new IllegalStateException(String.format(
                        "[%s] is already mapped to the path [%s]",
                        existingUiForPath.getCanonicalName(), path));
            }
            logger.debug("Mapping Vaadin UI [{}] to path [{}]",
                    uiClass.getCanonicalName(), path);
            mapPathToUI(path, uiClass);
            mapToDefaultViewField(uiClass);
        }
    }

    private void mapToDefaultViewField(Class<? extends UI> uiClass) {

        Field defaultViewField = null;

        for (java.lang.reflect.Field field : uiClass.getDeclaredFields()) {
            if (field.getAnnotation(DefaultView.class) == null) {
                continue;
            }

            checkArgument(defaultViewField == null, "more than one field annotated with @DefaultView in class " + uiClass);

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

                navigator.addProvider(InjectorHolder.getInjector().getInstance(ViewProvider.class));
                instance.setNavigator(navigator);
            }

            return instance;
        } finally {
            CurrentInstance.set(key, null);
        }
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        event.getSession().addUIProvider(this);
    }
}
