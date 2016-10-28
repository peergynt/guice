package com.vaadin.guice.i18n;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.Component;

import org.vaadin.i18n.annotation.Caption;
import org.vaadin.i18n.api.TranslationBinder;
import org.vaadin.i18n.api.Translator;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@UIScope
class GuiceTranslationBinder implements TranslationBinder {

    private final Multimap<String, Component> translatedComponentsByTemplate;

    @Inject
    private Translator translator;

    @Inject
    GuiceTranslationBinder(@AllTranslatedComponents Set<Component> components) {
        translatedComponentsByTemplate = HashMultimap.create(components.size(), 1);

        for (Component component : components) {
            final String template = component.getClass().getAnnotation(Caption.class).value();

            translatedComponentsByTemplate.put(
                    template,
                    component
            );
        }
    }

    public void bind() {
        for (Map.Entry<String, Collection<Component>> entry : translatedComponentsByTemplate.asMap().entrySet()) {

            String template = entry.getKey();
            Collection<Component> components = entry.getValue();

            String translation = translator.translate(template);

            for (Component component : components) {
                component.setCaption(translation);
            }
        }
    }

    @Override
    public void register(Component component, String template) {
        checkNotNull(component);
        checkArgument(!isNullOrEmpty(template));

        translatedComponentsByTemplate.put(template, component);
    }
}
