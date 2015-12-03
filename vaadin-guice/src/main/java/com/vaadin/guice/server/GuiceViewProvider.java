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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;

/**
 * A Vaadin {@link ViewProvider} that fetches the views from the guice application context. The
 * views must implement the {@link View} interface and be annotated with the {@link GuiceView}
 * annotation. <p> Use like this:
 *
 * <pre>
 * &#064;GuiceUI
 * public class MyUI extends UI {
 *
 *     &#064;Autowired
 *     GuiceViewProvider viewProvider;
 *
 *     protected void init(VaadinRequest vaadinRequest) {
 *         Navigator navigator = new Navigator(this, this);
 *         navigator.addProvider(viewProvider);
 *         setNavigator(navigator);
 *         // ...
 *     }
 * }
 * </pre>
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @see GuiceView
 */
@Singleton
public class GuiceViewProvider implements ViewProvider, SessionDestroyListener, SessionInitListener {

    private final Map<String, Class<? extends View>> viewNamesToViewClassesMap;
    private final Map<VaadinSession, Map<String, View>> viewsBySessionMap;
    private final Set<String> viewNames;
    private final TransactionBasedScoper viewScoper;

    public GuiceViewProvider(Set<Class<?>> viewClasses, TransactionBasedScoper viewScoper) {
        this.viewScoper = viewScoper;

        viewNamesToViewClassesMap = scanForViews(viewClasses);
        viewNames = viewNamesToViewClassesMap.keySet();

        viewsBySessionMap = new ConcurrentHashMap<VaadinSession, Map<String, View>>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Class<? extends View>> scanForViews(Set<Class<?>> viewClasses) {
        ImmutableMap.Builder<String, Class<? extends View>> viewMapBuilder = ImmutableMap.builder();

        for (Class viewClass : viewClasses) {
            checkArgument(
                    View.class.isAssignableFrom(viewClass),
                    "class %s has GuiceView-Annotation but does not implement com.vaadin.navigator.view",
                    viewClass
            );

            GuiceView annotation = (GuiceView) viewClass.getAnnotation(GuiceView.class);

            if (GuiceView.USE_CONVENTIONS.equals(annotation.name())) {

                String className = viewClass.getSimpleName();

                StringBuilder viewNameBuilder = new StringBuilder();

                for (int i = 0; i < className.length(); i++) {
                    char c = className.charAt(i);

                    if (isUpperCase(c)) {
                        viewNameBuilder.append('-');
                        viewNameBuilder.append(toLowerCase(c));
                    } else {
                        viewNameBuilder.append(c);
                    }
                }

                viewMapBuilder.put(viewNameBuilder.toString(), viewClass);
            } else {
                viewMapBuilder.put(annotation.name(), viewClass);
            }
        }

        return viewMapBuilder.build();
    }

    @Override
    public String getViewName(String viewAndParameters) {

        for (String viewName : viewNames) {
            if(viewAndParameters.startsWith(viewName)){
                return viewName;
            }
        }

        return viewAndParameters;
    }

    @Override
    public View getView(String viewName) {
        checkArgument(viewNames.contains(viewName), "%s is not a registered view-name", viewName);

        VaadinSession session = VaadinSession.getCurrent();

        Map<String, View> views = viewsBySessionMap.get(session);

        View view = views.get(viewName);

        if (view == null) {
            Class<? extends View> viewClass = viewNamesToViewClassesMap.get(viewName);

            checkArgument(viewClass != null, "no view for name %s registered", viewName);

            try {
                viewScoper.startScope();
                view = InjectorHolder.getInjector().getInstance(viewClass);
                views.put(viewName, view);
            } finally {
                viewScoper.endScope();
            }
        }

        return view;
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
        viewsBySessionMap.remove(event.getSession());
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        viewsBySessionMap.put(event.getSession(), new ConcurrentHashMap<String, View>(viewNamesToViewClassesMap.size()));
    }
}
