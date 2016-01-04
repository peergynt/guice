package com.vaadin.guice.annotation;

import com.google.inject.Module;

import com.vaadin.guice.server.GuiceVaadinServlet;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *  Configuration for {@link GuiceVaadinServlet}, attach directly to your GuiceVaadinServlet's declaration like
 *  <pre>
 *      <code>
             {@literal @}Configuration(modules={MyModule.class}, basePackage="com.myproject")
             {@literal @}WebServlet(urlPatterns = "/*", name = "MyServlet", asyncSupported = true)
             public static class MyServlet extends GuiceVaadinServlet {
             }
 *      </code>
 *  </pre>
 */
@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Configuration {

    /**
     * An array of classes for modules to be installed by guice
     * @return an array of classes for modules to be installed by guice
     */
    Class<? extends Module>[] modules();

    /**
     * A list of packages that is to be scanned for the guice-context. Sub-packages are included as well.
     * @return A list of packages that is to be scanned for the guice-context
     */
    String[] basePackage();
}
