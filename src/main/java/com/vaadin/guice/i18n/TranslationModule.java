package com.vaadin.guice.i18n;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import com.vaadin.guice.annotation.Caption;
import com.vaadin.guice.annotation.GuiceVaadinConfiguration;
import com.vaadin.guice.server.NeedsInjector;
import com.vaadin.guice.server.NeedsReflections;
import com.vaadin.ui.Component;

import org.reflections.Reflections;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * this Module cannot be used directly but needs to be subclassed and being provided with a {@link
 * Translator} class in order to set up properly. Also, make sure that the subclass is in a package
 * that is being included in {@link GuiceVaadinConfiguration#basePackages()} if you use the {@link
 * com.vaadin.guice.annotation.UIModule} annotation instead of {@link GuiceVaadinConfiguration#modules()}, as
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
public abstract class TranslationModule extends AbstractModule implements NeedsReflections, NeedsInjector {
    private final Class<? extends Translator> translatorClass;
    private Reflections reflections;
    private Provider<Injector> injectorProvider;

    public TranslationModule(Class<? extends Translator> translatorClass) {
        this.translatorClass = checkNotNull(translatorClass);
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

        bindListener(new AbstractMatcher<TypeLiteral<?>>() {
            @Override
            public boolean matches(TypeLiteral<?> typeLiteral) {
                return typeLiteral.getRawType().getAnnotation(Caption.class) != null;
            }
        }, new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                encounter.register(new InjectionListener<I>() {
                    @Override
                    public void afterInjection(I injectee) {
                        final Caption caption = injectee.getClass().getAnnotation(Caption.class);

                        Translator translator = injectorProvider.get().getInstance(Translator.class);

                        ((Component) injectee).setCaption(translator.translate(caption.value()));
                    }
                });
            }
        });
    }

    public void setReflections(Reflections reflections) {
        this.reflections = reflections;
    }

    public void setInjectorProvider(Provider<Injector> injectorProvider) {
        this.injectorProvider = injectorProvider;
    }
}
