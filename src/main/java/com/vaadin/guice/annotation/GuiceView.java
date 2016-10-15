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
 * &#064;GuiceView
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
     * The name of the view. This is the name that is to be passed to the {@link
     * com.vaadin.navigator.Navigator} when navigating to the view. There can be multiple views with
     * the same name as long as they belong to separate UI subclasses. If no value is given,
     * "" will be assigned which means that the view will be the 'default' view that the navigator
     * initially will navigate to.
     */
    String value() default "";
}
