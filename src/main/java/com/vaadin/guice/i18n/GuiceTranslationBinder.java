package com.vaadin.guice.i18n;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.Component;

import org.vaadin.i18n.annotation.Caption;
import org.vaadin.i18n.api.TranslationBinder;
import org.vaadin.i18n.api.Translator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@UIScope
class GuiceTranslationBinder implements TranslationBinder {

    private final Map<String, Component> translatedComponentsByTemplate;
    @Inject
    private Translator translator;

    @Inject
    GuiceTranslationBinder(@AllTranslatedComponents Set<Component> components) {
        translatedComponentsByTemplate = new HashMap<String, Component>(components.size());

        for (Component component : components) {
            translatedComponentsByTemplate.put(
                    component.getClass().getAnnotation(Caption.class).value(),
                    component
            );
        }
    }

    public void bind() {
        for (Map.Entry<String, Component> entry : translatedComponentsByTemplate.entrySet()) {
            String template = entry.getKey();
            Component component = entry.getValue();

            String translation = translator.translate(template);

            component.setCaption(translation);
        }
    }

    @Override
    public void register(Component component, String s) {
        checkNotNull(component);
        checkArgument(!isNullOrEmpty(s));

        translatedComponentsByTemplate.put(s, component);
    }
}
