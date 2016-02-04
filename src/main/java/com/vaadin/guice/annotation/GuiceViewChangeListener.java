package com.vaadin.guice.annotation;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that adds a ViewChangeListener to every {@see com.vaadin.navigation.Navigator} created by
 * guice-vaadin via {@see ViewContainer}.
 *
 * <pre>
 * &#064;GuiceViewChangeListener
 * public class MyViewChangeListener implements ViewChangeListener{
 *    boolean beforeViewChange(ViewChangeListener.ViewChangeEvent event){
 *        //before a view change
 *    }
 *
 *    void afterViewChange(ViewChangeListener.ViewChangeEvent event){
 *        //after a view change
 *    }
 * }
 * </pre>
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
@ScopeAnnotation
public @interface GuiceViewChangeListener {
}