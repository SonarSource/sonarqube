/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.web.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @deprecated since 2.0, use the lib sonar-gwt-api 
 */
@Deprecated
public abstract class AbstractPage implements EntryPoint {

  protected void displayView(Widget widget) {
    Element loading = DOM.getElementById("loading");
    if (loading != null) {
      DOM.removeChild(getRootPanel().getElement(), loading);
    }
    getRootPanel().add(widget);
  }

  protected RootPanel getRootPanel() {
    RootPanel rootPanel = RootPanel.get("gwtpage-" + GWT.getModuleName());
    if (rootPanel == null) {
      rootPanel = RootPanel.get("gwtpage");
    }
    return rootPanel;
  }
}
