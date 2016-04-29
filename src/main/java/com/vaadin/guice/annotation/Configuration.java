package com.vaadin.guice.annotation;

import com.google.common.base.Optional;
import com.google.inject.Module;

import com.vaadin.guice.access.ViewAccessControl;
import com.vaadin.guice.access.ViewInstanceAccessControl;
import com.vaadin.guice.server.GuiceVaadinServlet;
import com.vaadin.navigator.View;
import com.vaadin.ui.UI;

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
     * An array of classes for modules to be installed by guice
     */
    Class<? extends Module>[] modules();

    /**
     * A list of packages that is to be scanned for the guice-context. Sub-packages are included as
     * well.
     */
    String[] basePackages();

    /**
     * the {@link ViewAccessControl} that restricts the navigation flow so that only users with
     * proper permissions can visit restricted views
     */
    Class<? extends ViewAccessControl> viewAccessControl() default ViewAccessControlNoImpl.class;

    /**
     * the {@link ViewInstanceAccessControl} that restricts the navigation flow so that only users with
     * proper permissions can visit restricted views
     */
    Class<? extends ViewInstanceAccessControl> viewInstanceAccessControl() default ViewInstanceAccessControlNoImpl.class;

    class ViewAccessControlNoImpl implements ViewAccessControl{
        @Override
        public boolean isAccessGranted(UI ui, String beanName) {
            return true;
        }
    }

    class ViewInstanceAccessControlNoImpl implements ViewInstanceAccessControl{
        @Override
        public boolean isAccessGranted(UI ui, String beanName, View view) {
            return true;
        }
    }
}
