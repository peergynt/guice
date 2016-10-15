package com.vaadin.guice.annotation;

import com.google.inject.ScopeAnnotation;

import com.vaadin.guice.server.GuiceNavigator;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.ui.Component;

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

    /*
    * the UI's default view container for navigation.
    * ViewContainers are used by a UI's {@link com.vaadin.navigator.Navigator}. A view container must
    * implement one of the following interface:
    * <p>
    *     <ul>
    *         <li>{@link com.vaadin.ui.ComponentContainer}</li>
    *         <li>{@link com.vaadin.ui.SingleComponentContainer}</li>
    *         <li>{@link com.vaadin.navigator.ViewDisplay}</li>
    *     </ul>
    * <p>
    */
    Class<? extends Component> viewContainer() default Component.class;

    /**
     * the {@link Navigator} that is to be used for navigation
     */
    Class<? extends GuiceNavigator> navigator() default GuiceNavigator.class;

    /**
     * the {@link View} that should be displayed in case of an error, see {@link
     * Navigator#setErrorView(Class)}
     */
    Class<? extends View> errorView() default View.class;
}
