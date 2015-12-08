package com.vaadin.guice.annotation;

import com.google.inject.Module;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Configuration {
    Class<? extends Module>[] modules();

    String[] basePackage();
}
