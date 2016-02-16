package com.vaadin.guice.server;

import com.vaadin.ui.UI;

public interface CurrentUIProvider {
    UI getCurrentUI();
}
