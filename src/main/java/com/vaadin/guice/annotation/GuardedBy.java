package com.vaadin.guice.annotation;

import com.vaadin.guice.security.Guard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GuardedBy {
    Class<? extends Guard>[] value();
}
