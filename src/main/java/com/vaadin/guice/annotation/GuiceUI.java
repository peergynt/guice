package com.vaadin.guice.annotation;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be put on {@link com.vaadin.ui.UI}-subclasses that are to be automatically detected
 * and configured by guice. Use it like this:
 *
 * <pre>
 * &#064;GuiceUI
 * public class MyRootUI extends UI {
 *     // ...
 * }
 * </pre>
 *
 * Or like this, if you want to map your UI to another URL (for example if you are having multiple
 * UI subclasses in your application):
 *
 * <pre>
 * &#064;GuiceUI(path = &quot;/myPath&quot;)
 * public class MyUI extends UI {
 *     // ...
 * }
 * </pre>
 *
 * The annotated UI will automatically be placed in the {@link UIScope}, so there is no need to add
 * that annotation explicitly.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
@ScopeAnnotation
public @interface GuiceUI {

    /**
     * The path to which the UI will be bound. For example, a value of {@code "/myUI"} would be
     * mapped to {@code "/myContextPath/myVaadinServletPath/myUI"}. An empty string (default) will
     * map the UI to the root of the servlet. Within a web application, there must not be multiple
     * UI sub classes with the same path.
     */
    String path() default "";
}
