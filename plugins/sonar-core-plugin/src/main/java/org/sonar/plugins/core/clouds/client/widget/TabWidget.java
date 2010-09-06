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
package org.sonar.plugins.core.clouds.client.widget;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

public class TabWidget extends Composite {

  private TabPanel tab = new TabPanel();

  private Integer nbTab;
  private final Integer defaultSelectedTabPosition = 0;
  private String selectedTabId;
  private int selectedIndex;

  public TabWidget(final SelectionHandler<Integer> selectionListener) {
    nbTab = 0;
    initWidget(tab);
    tab.setWidth("100%");

    tab.addSelectionHandler(new SelectionHandler<Integer>() {
      public void onSelection(SelectionEvent<Integer> event) {
        selectedTabId = tab.getWidget(event.getSelectedItem()).getElement().getId().replace("_tab_content", "");
        selectedIndex = event.getSelectedItem();
        selectionListener.onSelection(event);
      }
    });

  }

  public String getSelectedTabId() {
    return selectedTabId;
  }

  public Widget getSelectedWidget() {
    return tab.getWidget(selectedIndex);
  }

  public void addTab(Widget widget, String tabName, String id) {
    widget.getElement().setId(id + "_tab_content");
    tab.add(widget, createTabLabel(tabName, id));
    if (nbTab.equals(defaultSelectedTabPosition)) {
      tab.selectTab(defaultSelectedTabPosition);
    }
    nbTab++;
  }

  private Label createTabLabel(String tabName, String id) {
    Label tabLabel = new Label(tabName);
    tabLabel.getElement().setId(id + "_tab_title");
    tabLabel.addStyleName("tab_title");
    return tabLabel;
  }

}
