package com.vaadin.guice.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Annotation to be placed on {@link com.google.inject.Module}-classes that should be loaded
 * dynamically by the {@link com.vaadin.guice.server.GuiceVaadinServlet}. Bindings from dynamically
 * loaded modules will override bindings from "statically loaded" modules that are listed in {@link
 * GuiceVaadinConfiguration#modules()}.
 *
 * <pre>
 * &#064;UIModule
 * public class MyDynamicallyLoadedModule extends AbstractModule {
 *     // ...
 * }
 * </pre>
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@Target({ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
public @interface UIModule {
}
