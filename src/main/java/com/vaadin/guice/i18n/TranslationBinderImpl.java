package com.vaadin.guice.i18n;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.Caption;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.Component;

import java.util.Set;

@UIScope
class TranslationBinderImpl implements TranslationBinder {

    @AllTranslatedComponents
    @Inject
    private Set<Component> components;

    @Inject
    private Translator translator;

    public void bind() {
        for (Component component : components) {

            Caption caption = component.getClass().getAnnotation(Caption.class);

            String translation = translator.translate(caption.value());

            component.setCaption(translation);
        }
    }
}
