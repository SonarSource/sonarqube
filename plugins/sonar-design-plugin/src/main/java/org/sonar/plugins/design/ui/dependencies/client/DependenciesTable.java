/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.design.ui.dependencies.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Links;
import org.sonar.gwt.ui.Icons;
import org.sonar.wsclient.services.Dependency;

public class DependenciesTable extends Composite {

  private HorizontalPanel panel;

  public DependenciesTable() {
    panel = new HorizontalPanel();
    panel.setStylePrimaryName("dependencies");
    initWidget(panel);
  }


  public void display(final Data data) {
    panel.clear();
    if (data.canDisplay()) {
      panel.add(createIncomingColumn(data));
      panel.add(createOutgoingColumn(data));
    } else {
      panel.add(new Label(Dictionary.getDictionary("l10n").get("noData")));
    }
  }


  private Panel createIncomingColumn(Data data) {
    FlexTable grid = new FlexTable();
    grid.setStyleName("col");
    grid.setWidget(0, 1, new HTML(Dictionary.getDictionary("l10n").get("depsTab.afferentCouplings") + ": <b>" + data.getResource().getMeasureIntValue("ca") + "</b>"));
    grid.getRowFormatter().setStyleName(0, "coltitle");

    int row = 1;
    for (Dependency dependency : data.getDependencies()) {
      if (data.getResourceId() == dependency.getToId()) {
        addDependencyRow(grid, row, dependency.getFromId(), dependency.getFromName() + " (" + dependency.getWeight() + ")");
        grid.setWidget(row, 0, Icons.forQualifier(dependency.getFromQualifier()).createImage());
        row++;
      }
    }

    return grid;
  }

  private Panel createOutgoingColumn(Data data) {
    FlexTable grid = new FlexTable();
    grid.setStyleName("col");
    grid.setWidget(0, 1, new HTML(Dictionary.getDictionary("l10n").get("depsTab.efferentCouplings") + ": <b>" + data.getResource().getMeasureIntValue("ce") + "</b>"));
    grid.getRowFormatter().setStyleName(0, "coltitle");

    int row = 1;
    for (Dependency dependency : data.getDependencies()) {
      if (data.getResourceId() == dependency.getFromId()) {
        addDependencyRow(grid, row, dependency.getToId(), dependency.getToName() + " (" + dependency.getWeight() + ")");
        grid.setWidget(row, 0, Icons.forQualifier(dependency.getToQualifier()).createImage());
        row++;
      }
    }

    return grid;
  }

  private void addDependencyRow(final FlexTable grid, final int row, final long resourceId, final String name) {
    Label link = new Label(name);
    link.setStyleName("link");
    link.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent clickEvent) {
        Links.openResourcePopup(String.valueOf(resourceId));
      }
    });
    grid.setWidget(row, 1, link);
  }
}