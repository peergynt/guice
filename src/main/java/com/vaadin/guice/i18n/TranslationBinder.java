package com.vaadin.guice.i18n;

import com.vaadin.guice.annotation.Caption;

/**
 * A TranslationBinder should be injected on every part of your application where you want your
 * translations to be bound to the {@link com.vaadin.ui.Component}s annotated with {@link Caption}
 */
public interface TranslationBinder {

    /**
     * calling this method will ask the {@link Translator} that is being configured by the {@link
     * TranslationModule} for translations of every {@link Caption#value()} in the current {@link
     * com.vaadin.ui.UI}'s scope and feed these translations to the {@link
     * com.vaadin.ui.Component#setCaption(String)} methods of the annotated objects.
     */
    void bind();
}