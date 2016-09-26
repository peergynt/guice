package com.vaadin.guice.annotation;

import com.google.inject.Module;

import com.vaadin.guice.server.GuiceVaadinServlet;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Configuration for {@link GuiceVaadinServlet}, attach directly to your GuiceVaadinServlet's
 * declaration like
 * <pre>
 *      <code>
 * {@literal @}Configuration(modules={MyModule.class}, basePackages="com.myproject")
 * {@literal @}WebServlet(urlPatterns = "/*", name = "MyServlet", asyncSupported = true)
 * public static class MyServlet extends GuiceVaadinServlet {
 * }
 *      </code>
 *  </pre>
 */
@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Configuration {

    /**
     * An array of classes for modules to be installed by guice. Each of these classes must have
     * a default ( no-args ) constructor
     */
    Class<? extends Module>[] modules() default {};

    /**
     * A list of packages that is to be scanned for the guice-context. Sub-packages are included as
     * well.
     */
    String[] basePackages();
}
