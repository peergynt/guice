package com.vaadin.guice.server;

import com.google.inject.Injector;

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.server.VaadinServlet;

import org.reflections.Reflections;

import javax.servlet.ServletException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Subclass of the standard {@link com.vaadin.server.VaadinServlet Vaadin servlet} that adds a
 * {@link GuiceUIProvider} to every new Vaadin session
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
public class GuiceVaadinServlet extends VaadinServlet {

    private final GuiceVaadin guiceVaadin;

    public GuiceVaadinServlet() {
        Configuration annotation = getClass().getAnnotation(Configuration.class);

        checkArgument(
                annotation != null,
                "GuiceVaadinServlet cannot be used without 'Configuration' annotation"
        );

        checkArgument(
                annotation.basePackages().length > 0,
                "at least on 'basePackages'-parameter expected in Configuration of " + getClass()
        );

        Reflections reflections = new Reflections((Object[]) annotation.basePackages());

        try {
            this.guiceVaadin = new GuiceVaadin(reflections, annotation.modules());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void servletInitialized() throws ServletException {
        guiceVaadin.vaadinInitialized();
    }

    protected Injector getInjector() {
        return guiceVaadin.getInjector();
    }

}
