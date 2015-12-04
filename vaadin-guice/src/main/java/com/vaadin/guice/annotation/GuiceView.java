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

import com.vaadin.guice.server.GuiceViewProvider;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be placed on {@link com.vaadin.navigator.View}-classes that should be handled by
 * the {@link GuiceViewProvider}. <p> This annotation is also a stereotype annotation, so guice will
 * automatically detect the annotated classes. By default, this annotation also puts the view into
 * the {@link com.vaadin.guice.annotation.ViewScope view scope}. You can override this by using
 * another scope annotation, such as {@link UIScope the UI scope}, on your view class. <b>However,
 * the singleton scope will not work!</b> <p> This is an example of a view that is mapped to an
 * empty view name and is available for all UI subclasses in the application:
 *
 * <pre>
 * &#064;GuiceView(name = &quot;&quot;)
 * public class MyDefaultView extends CustomComponent implements View {
 *     // ...
 * }
 * </pre>
 *
 * This is an example of a view that is only available to a specified UI subclass:
 *
 * <pre>
 * &#064;GuiceView(name = &quot;myView&quot;, ui = MyUI.class)
 * public class MyView extends CustomComponent implements View {
 *     // ...
 * }
 * </pre>
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 */
@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
public @interface GuiceView {

    /**
     * USE_CONVENTIONS is treated as a special case that will cause the automatic View mapping to
     * occur.
     */
    String USE_CONVENTIONS = "USE CONVENTIONS";

    /**
     * The name of the view. This is the name that is to be passed to the {@link
     * com.vaadin.navigator.Navigator} when navigating to the view. There can be multiple views with
     * the same name as long as they belong to separate UI subclasses.
     *
     * If the default value {@link #USE_CONVENTIONS} is used, the name of the view is derived from
     * the class name so that e.g. UserDetailView becomes "user-detail". Although auto-generated
     * view names are supported, using explicit naming of views is strongly recommended.
     */
    String name() default USE_CONVENTIONS;
}
