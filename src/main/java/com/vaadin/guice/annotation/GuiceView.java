package com.vaadin.guice.annotation;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be placed on {@link com.vaadin.navigator.View}-classes that should be handled by
 * the {@link com.vaadin.navigator.ViewProvider}.
 *
 * <pre>
 * &#064;GuiceView(name = &quot;&quot;)
 * public class MyDefaultView extends CustomComponent implements View {
 *     // ...
 * }
 * </pre>
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
@ScopeAnnotation
public @interface GuiceView {

    /**
     * USE_CONVENTIONS is treated as a special case that will cause the automatic View mapping to
     * occur.
     */
    String USE_CONVENTIONS = "USE CONVENTIONS";

    /**
     * The name of the view. This is the name that is to be passed to the {@link
     * com.vaadin.navigator.Navigator} when navigating to the view. There can be multiple views with
     * the same name as long as they belong to separate UI subclasses.
     *
     * If the default value {@link #USE_CONVENTIONS} is used, the name of the view is derived from
     * the class name so that e.g. UserDetailView becomes "user-detail". Although auto-generated
     * view names are supported, using explicit naming of views is strongly recommended.
     */
    String name() default USE_CONVENTIONS;

    /*
    * determining if this view is navigated to in case an error occurs.
    * */
    boolean isErrorView() default false;
}
