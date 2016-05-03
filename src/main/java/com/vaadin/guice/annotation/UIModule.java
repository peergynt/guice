/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.guice.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Annotation to be placed on {@link com.google.inject.Module}-classes that should be loaded
 * dynamically by the {@link com.vaadin.guice.server.GuiceVaadinServlet}. Bindings from dynamically
 * loaded modules will override bindings from "statically loaded" modules that are listed in {@link
 * Configuration#modules()}.
 *
 * <pre>
 * &#064;UIModule
 * public class MyDynamicallyLoadedModule extends AbstractModule {
 *     // ...
 * }
 * </pre>
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@Target({ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
public @interface UIModule {
}
