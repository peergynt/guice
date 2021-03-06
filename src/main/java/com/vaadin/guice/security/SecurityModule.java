package com.vaadin.guice.security;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.server.NeedsInjector;
import com.vaadin.guice.server.NeedsReflections;
import com.vaadin.navigator.View;
import com.vaadin.ui.Component;

import org.reflections.Reflections;
import org.vaadin.security.annotation.Restricted;
import org.vaadin.security.api.PermissionEnforcer;
import org.vaadin.security.api.PermissionEvaluator;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.inject.name.Names.named;

public abstract class SecurityModule extends AbstractModule implements NeedsReflections, NeedsInjector {

    private final Class<? extends PermissionEvaluator> permissionEvaluatorClass;
    private final Class<? extends View> permissionDeniedView;
    private Reflections reflections;
    private Provider<Injector> injectorProvider;

    public SecurityModule(Class<? extends PermissionEvaluator> permissionEvaluatorClass, Class<? extends View> permissionDeniedView) {
        this.permissionDeniedView = permissionDeniedView;
        this.permissionEvaluatorClass = permissionEvaluatorClass;
    }

    public SecurityModule(Class<? extends PermissionEvaluator> permissionEvaluatorClass) {
        this(permissionEvaluatorClass, null);
    }

    public SecurityModule() {
        this(null, null);
    }

    @SuppressWarnings("unchecked")
    protected void configure() {

        if (permissionEvaluatorClass != null) {
            bind(PermissionEvaluator.class).to(permissionEvaluatorClass);
            bind(PermissionEnforcer.class).to(GuicePermissionEnforcer.class);

            final Set<Class<?>> restrictedComponentClasses = reflections.getTypesAnnotatedWith(Restricted.class);

            Multibinder<Component> restrictedComponents = Multibinder.newSetBinder(binder(), Component.class, AllRestrictedComponents.class);

            for (Class<?> restrictedComponentClass : restrictedComponentClasses) {
                checkState(
                        Component.class.isAssignableFrom(restrictedComponentClass),
                        "%s has @NeedsPermission annotation but is not itself a com.vaadin.ui.Component",
                        restrictedComponentClass
                );

                final Restricted annotation = restrictedComponentClass.getAnnotation(Restricted.class);

                checkState(!annotation.value().isEmpty(), "%s has @Restricted annotation with empty value");

                restrictedComponents.addBinding().to((Class<? extends Component>) restrictedComponentClass);
            }

            bindListener(
                    new AbstractMatcher<TypeLiteral<?>>() {
                        @Override
                        public boolean matches(TypeLiteral<?> typeLiteral) {
                            return typeLiteral.getRawType().getAnnotation(Restricted.class) != null;
                        }
                    },
                    new TypeListener() {
                        @Override
                        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                            encounter.register(
                                    new InjectionListener<I>() {
                                        @Override
                                        public void afterInjection(I injectee) {
                                            final Restricted annotation = injectee.getClass().getAnnotation(Restricted.class);

                                            PermissionEvaluator permissionEvaluator = injectorProvider.get().getInstance(PermissionEvaluator.class);

                                            ((Component) injectee).setVisible(permissionEvaluator.hasPermission(annotation.value()));
                                        }
                                    }
                            );
                        }
                }
            );
        }

        if (permissionDeniedView != null) {
            final GuiceView guiceView = permissionDeniedView.getAnnotation(GuiceView.class);

            checkArgument(
                    guiceView != null,
                    "% is provided as PermissionDeniedView, but has no GuiceView annotation",
                    permissionDeniedView
            );

            bind(String.class)
                    .annotatedWith(named("guice_security_permission_denied_view"))
                    .toInstance(permissionDeniedView.getAnnotation(GuiceView.class).value());
        } else {
            bind(String.class)
                    .annotatedWith(named("guice_security_permission_denied_view"))
                    .toInstance("");
        }
    }

    public void setInjectorProvider(Provider<Injector> injectorProvider) {
        this.injectorProvider = injectorProvider;
    }

    public void setReflections(Reflections reflections) {
        reflections.merge(new Reflections("com.vaadin.guice.security"));
        this.reflections = reflections;
    }
}
