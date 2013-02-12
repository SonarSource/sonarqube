/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.design.ui.page.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Configuration;
import org.sonar.gwt.Links;
import org.sonar.gwt.ui.Icons;
import org.sonar.gwt.ui.Loading;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.AbstractListCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Dependency;
import org.sonar.wsclient.services.DependencyQuery;
import org.sonar.wsclient.services.Resource;

import java.util.List;

public final class DependencyInfo extends Composite {

  private static final DependencyInfo INSTANCE = new DependencyInfo();

  private VerticalPanel panel;
  private Loading loading = new Loading();
  private String currentDependencyId = null;
  private boolean popupMode = false;

  private DependencyInfo() {
    panel = new VerticalPanel();
    initWidget(panel);
  }

  public static DependencyInfo getInstance() {
    return INSTANCE;
  }


  public void showOrPopup(String dependencyId) {
    if (popupMode) {
      Window.open(Links.urlForResourcePage(Configuration.getResourceId(), DesignPage.GWT_ID, "layout=false&depId=" + dependencyId),
        "dependency", Links.DEFAULT_POPUP_HTML_FEATURES);

    } else {
      INSTANCE.show(dependencyId);
    }
  }

  public void show(String dependencyId) {
    panel.clear();
    currentDependencyId = dependencyId;
    if (dependencyId != null) {
      panel.add(loading);
      loadDependency(dependencyId);
    }
  }

  public DependencyInfo setPopupMode(boolean b) {
    this.popupMode = b;
    return this;
  }

  public void popup() {
    popupMode = true;
    panel.clear();
    showOrPopup(currentDependencyId);
  }

  private void setLoaded() {
    loading.removeFromParent();
  }

  private void loadDependency(String dependencyId) {
    DependencyQuery query = DependencyQuery.createForId(dependencyId);
    Sonar.getInstance().find(query, new AbstractCallback<Dependency>() {
      @Override
      protected void doOnResponse(Dependency dependency) {
        if (dependency == null) {
          setLoaded();
          panel.add(new Label(Dictionary.getDictionary("l10n").get("noData")));
        } else {
          loadSubDependencies(dependency);
        }
      }

      @Override
      protected void doOnError(int errorCode, String errorMessage) {
        super.doOnError(errorCode, errorMessage);
      }
    });
  }

  private void loadSubDependencies(final Dependency dependency) {
    DependencyQuery query = DependencyQuery.createForSubDependencies(dependency.getId());
    Sonar.getInstance().findAll(query, new AbstractListCallback<Dependency>() {

      @Override
      protected void doOnResponse(final List<Dependency> subDependencies) {
        Grid table = new Grid(subDependencies.size() + 1, 5);
        table.setStyleName("depInfo");
        createHeader(dependency, table);

        for (int row = 0; row < subDependencies.size(); row++) {
          Dependency dep = subDependencies.get(row);
          table.setWidget(row + 1, 0, new HTML(Icons.forQualifier(dep.getFromQualifier()).getHTML()));
          if (Resource.QUALIFIER_FILE.equals(dep.getFromQualifier()) || Resource.QUALIFIER_CLASS.equals(dep.getFromQualifier())) {
            table.setWidget(row + 1, 1, createLink(dep.getFromId(), dep.getFromName()));
          } else {
            table.setText(row + 1, 1, dep.getFromName());
          }
          table.setText(row + 1, 2, "  " + dep.getUsage() + "  ");
          table.setWidget(row + 1, 3, new HTML(Icons.forQualifier(dep.getToQualifier()).getHTML()));
          if (Resource.QUALIFIER_FILE.equals(dep.getToQualifier()) || Resource.QUALIFIER_CLASS.equals(dep.getToQualifier())) {
            table.setWidget(row + 1, 4, createLink(dep.getToId(), dep.getToName()));
          } else {
            table.setText(row + 1, 4, dep.getToName());
          }
        }


        panel.clear();
        if (!popupMode) {
          panel.add(createNewWindowLink());
        }
        panel.add(table);
      }
    });
  }

  private Label createLink(final long resourceId, final String resourceName) {
    Label link = new Label(resourceName);
    link.setStyleName("link");
    link.addClickHandler(new ClickHandler() {

      public void onClick(ClickEvent event) {
        Links.openResourcePopup(String.valueOf(resourceId));
      }
    });
    return link;
  }

  private void createHeader(final Dependency dependency, final Grid grid) {
    grid.getRowFormatter().setStyleName(0, "depInfoHeader");

    grid.setWidget(0, 0, Icons.forQualifier(dependency.getFromQualifier()).createImage());
    grid.setText(0, 1, dependency.getFromName());

    grid.setWidget(0, 3, Icons.forQualifier(dependency.getToQualifier()).createImage());
    grid.setText(0, 4, dependency.getToName());
  }

  private Widget createNewWindowLink() {
    Label popup = new Label(Dictionary.getDictionary("l10n").get("newWindow"));
    popup.setStyleName("newwindow");
    popup.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        popup();
      }
    });
    return popup;
  }
}
