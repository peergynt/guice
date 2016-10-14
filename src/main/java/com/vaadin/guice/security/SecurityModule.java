package com.vaadin.guice.security;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.NeedsPermission;
import com.vaadin.guice.server.NeedsReflections;
import com.vaadin.navigator.View;
import com.vaadin.ui.Component;

import org.reflections.Reflections;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.inject.name.Names.named;

public abstract class SecurityModule extends AbstractModule implements NeedsReflections {

    private final Class<? extends PermissionEvaluator> permissionEvaluatorClass;
    private final Class<? extends View> permissionDeniedView;
    private Reflections reflections;

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
            bind(PermissionEnforcer.class).to(PermissionEnforcerImpl.class);

            final Set<Class<?>> needsPermissions = reflections.getTypesAnnotatedWith(NeedsPermission.class);

            Multibinder<Component> restrictedComponents = Multibinder.newSetBinder(binder(), Component.class, AllRestrictedComponents.class);

            for (Class<?> needsPermission : needsPermissions) {
                checkState(Component.class.isAssignableFrom(needsPermission));
                restrictedComponents.addBinding().to((Class<? extends Component>) needsPermission);
            }
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
                    .toInstance(guiceView.name());
        } else {
            bind(String.class)
                    .annotatedWith(named("guice_security_permission_denied_view"))
                    .toInstance("");
        }

    }

    public void setReflections(Reflections reflections) {
        reflections.merge(new Reflections("com.vaadin.guice.security"));
        this.reflections = reflections;
    }
}
