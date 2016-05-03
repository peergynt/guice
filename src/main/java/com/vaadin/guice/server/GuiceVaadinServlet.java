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
package com.vaadin.guice.server;

import com.vaadin.guice.annotation.Configuration;
import com.vaadin.server.VaadinServlet;

import org.reflections.Reflections;

import javax.servlet.ServletException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Subclass of the standard {@link com.vaadin.server.VaadinServlet Vaadin servlet} that adds a
 * {@link GuiceUIProvider} to every new Vaadin session
 * @author Bernd Hopp (bernd@vaadin.com)
 */
public class GuiceVaadinServlet extends VaadinServlet {

    private final GuiceVaadin guiceVaadin;

    public GuiceVaadinServlet() {
        Configuration annotation = getClass().getAnnotation(Configuration.class);

        checkArgument(
                annotation != null,
                "GuiceVaadinServlet cannot be used without 'Configuration' annotation"
        );

        checkArgument(
                annotation.basePackages().length > 0,
                "at least on 'basePackages'-parameter expected in Configuration of " + getClass()
        );

        Reflections reflections = new Reflections((Object[]) annotation.basePackages());

        this.guiceVaadin = new GuiceVaadin(reflections, annotation.modules());
    }

    @Override
    protected void servletInitialized() throws ServletException {
        guiceVaadin.vaadinInitialized();
    }
}
