package com.vaadin.guice.i18n;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import com.vaadin.guice.annotation.Caption;
import com.vaadin.guice.annotation.Configuration;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.guice.server.NeedsReflections;
import com.vaadin.ui.Component;

import org.reflections.Reflections;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * this Module cannot be used directly but needs to be subclassed and being provided with a {@link
 * Translator} class in order to set up properly. Also, make sure that the subclass is in a package
 * that is being included in {@link Configuration#basePackages()} if you use the {@link
 * com.vaadin.guice.annotation.UIModule} annotation instead of {@link Configuration#modules()}, as
 * in the code below.
 *
 * <pre>
 * @UIModule
 * class MyTranslationModule extends TranslationModule {
 *     MyTranslationModule(){
 *         super(MyTranslator.class);
 *     }
 * }
 * </pre>
 */
public abstract class TranslationModule extends AbstractModule implements NeedsReflections {
    private final Class<? extends Translator> translatorClass;
    private Reflections reflections;

    public TranslationModule(Class<? extends Translator> translatorClass) {

        this.translatorClass = checkNotNull(translatorClass);

        checkArgument(
                translatorClass.getAnnotation(UIScope.class) != null,
                "%s needs to be put in UIScope, please add @UIScope annotation to it",
                translatorClass
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {

        final Set<Class<?>> translatedTypes = reflections.getTypesAnnotatedWith(Caption.class);

        Multibinder<Component> translatedComponentsMultibinder = Multibinder.newSetBinder(binder(), Component.class, AllTranslatedComponents.class);

        for (Class<?> translatedTypeRaw : translatedTypes) {
            checkState(
                    Component.class.isAssignableFrom(translatedTypeRaw),
                    "%s is annotated with @Caption, must implement com.vaadin.ui.Component",
                    translatedTypeRaw
            );

            translatedComponentsMultibinder.addBinding().to((Class<? extends Component>) translatedTypeRaw);
        }

        bind(Translator.class).to(translatorClass);
        bind(TranslationBinder.class).to(TranslationBinderImpl.class);
    }

    @Override
    public void setReflections(Reflections reflections) {
        this.reflections = reflections;
    }
}
