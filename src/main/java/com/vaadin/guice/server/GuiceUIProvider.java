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
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vaadin.guice.server.NavigatorUtil.createNavigator;
import static com.vaadin.guice.server.PathUtil.extractUIPathFromRequest;
import static com.vaadin.guice.server.ReflectionUtils.detectUIs;
import static com.vaadin.guice.server.ReflectionUtils.findErrorView;

/**
 * Vaadin {@link com.vaadin.server.UIProvider} that looks up UI classes from the Guice application
 * context. The UI classes must be annotated with {@link GuiceUI}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @author Bernd Hopp (bernd@vaadin.com)
 */
class GuiceUIProvider extends UIProvider implements SessionInitListener {

    private final Map<String, Class<? extends UI>> pathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();
    private final Map<String, Class<? extends UI>> wildcardPathToUIMap = new ConcurrentHashMap<String, Class<? extends UI>>();
    private final Map<Class<? extends UI>, Field> uiToDefaultViewField = new ConcurrentHashMap<Class<? extends UI>, Field>();
    private final Set<Class<? extends ViewChangeListener>> viewChangeListeners;
    private final GuiceViewProvider viewProvider;
    private final UIScoper uiScoper;
    private Optional<Class<? extends View>> errorView;

    GuiceUIProvider(
            Set<Class<? extends UI>> uiClasses,
            Set<Class<? extends ViewChangeListener>> viewChangeListeners,
            GuiceViewProvider viewProvider,
            Set<Class<? extends View>> viewClasses,
            UIScoper uiScoper
    ) {
        this.viewProvider = viewProvider;
        this.uiScoper = uiScoper;

        detectUIs(uiClasses, pathToUIMap, wildcardPathToUIMap, uiToDefaultViewField);

        this.errorView = findErrorView(viewClasses);

        this.viewChangeListeners = viewChangeListeners;
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
                        "%s is annotated with @ViewContainer and therefore must not be null",
                        defaultViewField.getName()
                );

                Navigator navigator = createNavigator(instance, defaultView, errorView, viewChangeListeners);

                navigator.addProvider(viewProvider);

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

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        event.getSession().addUIProvider(this);
    }
}
