package com.vaadin.guice.security;

import com.google.inject.AbstractModule;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.server.NeedsReflections;
import com.vaadin.navigator.View;

import org.reflections.Reflections;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.name.Names.named;

public abstract class SecurityModule extends AbstractModule implements NeedsReflections {

    private final Class<? extends PermissionEvaluator> permissionEvaluatorClass;
    private final Class<? extends View> permissionDeniedView;

    protected SecurityModule(Class<? extends PermissionEvaluator> permissionEvaluatorClass, Class<? extends View> permissionDeniedView) {
        this.permissionDeniedView = permissionDeniedView;
        this.permissionEvaluatorClass = checkNotNull(permissionEvaluatorClass);
    }

    protected SecurityModule(Class<? extends PermissionEvaluator> permissionEvaluatorClass) {
        this(permissionEvaluatorClass, null);
    }

    protected void configure() {

        bind(PermissionEvaluator.class).to(permissionEvaluatorClass);
        bind(PermissionEnforcer.class).to(PermissionEnforcerImpl.class);

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
        }
    }

    public void setReflections(Reflections reflections) {
        reflections.merge(new Reflections("com.vaadin.guice.security"));
    }
}
