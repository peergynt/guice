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

import com.vaadin.server.UICreateEvent;
import com.vaadin.ui.UI;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uniquely identifies a UI instance for a given window/tab inside a session. This is basically a
 * wrapper for {@link com.vaadin.ui.UI#getUIId()}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Josh Long (josh@joshlong.com)
 */
public class UIID implements Serializable {

    private static final long serialVersionUID = -999655298640370601L;

    private final int uiId;

    public UIID(UICreateEvent createEvent) {
        this.uiId = createEvent.getUiId();
    }

    public UIID(UI ui) {
        checkNotNull(ui, "ui must not be null");
        checkArgument(ui.getUIId() > -1, "UIId of ui must not be -1");
        this.uiId = ui.getUIId();
    }

    public UIID(int uiId) {
        this.uiId = uiId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final UIID that = (UIID) o;

        return uiId == that.uiId;
    }

    @Override
    public int hashCode() {
        return uiId;
    }

    @Override
    public String toString() {
        return String.format("UI:%d", uiId);
    }
}
