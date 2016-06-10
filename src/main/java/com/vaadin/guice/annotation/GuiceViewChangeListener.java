package com.vaadin.guice.annotation;

import com.vaadin.ui.UI;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that adds a ViewChangeListener to every {@see com.vaadin.navigation.Navigator} created
 * by guice-vaadin via {@see ViewContainer}.
 *
 * <pre>
 * &#064;GuiceViewChangeListener(uis = MyUI.class) // will only be attached to MyUI
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
public @interface GuiceViewChangeListener {

    /**
     * A list of {@link UI}'s that the ViewChangeListener will be attached to.
     * If not set, the ViewChangeListener will be attached to all UI's
     */
    Class<? extends UI>[] applicableUIs() default {};
}