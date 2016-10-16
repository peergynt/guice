package com.vaadin.guice.bus;

import com.google.common.eventbus.Subscribe;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;

import java.lang.reflect.Method;

class SubscriberMethodsMatcher extends AbstractMatcher<TypeLiteral<?>> {
    @Override
    public boolean matches(TypeLiteral<?> typeLiteral) {

        //any class with at least on @Subscribe annotated method
        final Class<?> rawType = typeLiteral.getRawType();

        for (Method method : rawType.getMethods()) {
            if (method.getAnnotation(Subscribe.class) != null) {
                return true;
            }
        }

        return false;
    }
}
