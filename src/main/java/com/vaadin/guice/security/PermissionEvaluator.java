package com.vaadin.guice.security;

public interface PermissionEvaluator {
    boolean hasPermission(String permission);
}
