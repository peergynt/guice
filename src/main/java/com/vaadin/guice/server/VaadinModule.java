package com.vaadin.guice.server;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.guice.annotation.GuiceViews;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.guice.annotation.VaadinSessionScope;
import com.vaadin.guice.annotation.ViewScope;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

class VaadinModule extends AbstractModule {

    private final GuiceVaadin guiceVaadin;

    VaadinModule(GuiceVaadin guiceVaadin) {
        this.guiceVaadin = guiceVaadin;
    }

    @Override
    protected void configure() {
        bindScope(UIScope.class, guiceVaadin.getUiScoper());
        bindScope(GuiceUI.class, guiceVaadin.getUiScoper());
        bindScope(ViewScope.class, guiceVaadin.getViewScoper());
        bindScope(GuiceView.class, guiceVaadin.getUiScoper());
        bindScope(VaadinSessionScope.class, guiceVaadin.getVaadinSessionScoper());
        bind(UIProvider.class).toInstance(guiceVaadin.getGuiceUIProvider());
        bind(ViewProvider.class).toInstance(guiceVaadin.getViewProvider());

        bind(VaadinSession.class).toProvider(guiceVaadin.getVaadinSessionProvider());
        bind(UI.class).toProvider(guiceVaadin.getCurrentUIProvider());
        bind(VaadinService.class).toProvider(guiceVaadin.getVaadinServiceProvider());

        final Multibinder<View> viewMultibinder = Multibinder.newSetBinder(binder(), View.class, GuiceViews.class);

        for (Class<? extends View> guiceViewClass : guiceVaadin.getViews()) {
            viewMultibinder.addBinding().to(guiceViewClass);
        }
    }
}
