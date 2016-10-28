package com.vaadin.guice.annotation;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be placed on a Set of {@link com.vaadin.navigator.View}s that should contain all
 * known Views annotated with {@link GuiceView}
 *
 * &#064;GuiceView(name = &quot;myView&quot;, ui = MyUI.class) public class MyView extends
 * CustomComponent implements View { // ... } </pre>
 *
 * {@literal @}Inject {@literal @}GuiceViews private Set&lt;View&gt; guiceViews;
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
@BindingAnnotation
public @interface GuiceViews {
}
