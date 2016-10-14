package com.vaadin.guice.server;

import com.google.common.collect.ImmutableMap;

import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.i18n.TranslationBinder;
import com.vaadin.guice.security.PermissionEnforcer;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkState;
import static com.vaadin.guice.server.PathUtil.extractUIPathFromRequest;
import static com.vaadin.guice.server.PathUtil.preparePath;

/**
 * Vaadin {@link com.vaadin.server.UIProvider} that looks up UI classes from the Guice application
 * context. The UI classes must be annotated with {@link GuiceUI}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @author Bernd Hopp (bernd@vaadin.com)
 */
class GuiceUIProvider extends UIProvider {

    private final Map<String, Class<? extends UI>> pathToUIMap;
    private final Map<String, Class<? extends UI>> wildcardPathToUIMap;
    private final GuiceVaadin guiceVaadin;
    private final NavigatorManager navigatorManager;

    GuiceUIProvider(GuiceVaadin guiceVaadin) {
        this.guiceVaadin = guiceVaadin;
        Logger logger = Logger.getLogger(getClass().getName());

        logger.info("Checking the application context for Vaadin UIs");

        final HashMap<String, Class<? extends UI>> pathToUIMapCollector = new HashMap<String, Class<? extends UI>>();
        final Map<String, Class<? extends UI>> wildcardPathToUIMapCollector = new HashMap<String, Class<? extends UI>>();

        for (Class<? extends UI> uiClass : guiceVaadin.getUis()) {

            GuiceUI annotation = uiClass.getAnnotation(GuiceUI.class);

            if (annotation == null) {
                logger.log(Level.WARNING, "ignoring {0}, because it has no @GuiceUI annotation", new Object[]{uiClass});
                continue;
            }

            String path = annotation.path();
            path = preparePath(path);

            Class<? extends UI> existingUiForPath = pathToUIMapCollector.get(path);

            checkState(
                    existingUiForPath == null,
                    "[%s] is already mapped to the path [%s]",
                    existingUiForPath,
                    path
            );

            logger.log(Level.INFO, "Mapping Vaadin UI [{0}] to path [{1}]",
                    new Object[]{uiClass.getCanonicalName(), path});

            if (path.endsWith("/*")) {
                wildcardPathToUIMapCollector.put(path.substring(0, path.length() - 2),
                        uiClass);
            } else {
                pathToUIMapCollector.put(path, uiClass);
            }
        }

        if (pathToUIMapCollector.isEmpty()) {
            logger.log(Level.WARNING, "Found no Vaadin UIs in the application context");
        }

        this.navigatorManager = new NavigatorManager(guiceVaadin);

        this.pathToUIMap = ImmutableMap.copyOf(pathToUIMapCollector);
        this.wildcardPathToUIMap = ImmutableMap.copyOf(wildcardPathToUIMapCollector);
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
        try {
            guiceVaadin.getUiScoper().startInitialization();

            UI instance = guiceVaadin.assemble(event.getUIClass());

            navigatorManager.addNavigator(instance);

            if (guiceVaadin.isBound(PermissionEnforcer.class)) {
                guiceVaadin.assemble(PermissionEnforcer.class).enforce();
            }

            if (guiceVaadin.isBound(TranslationBinder.class)) {
                guiceVaadin.assemble(TranslationBinder.class).bind();
            }

            guiceVaadin.getUiScoper().endInitialization(instance);

            return instance;
        } catch (RuntimeException e) {
            guiceVaadin.getUiScoper().rollbackInitialization();
            throw e;
        }
    }
}
