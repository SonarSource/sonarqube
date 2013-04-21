/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.design.ui.libraries.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.gwt.AbstractListCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.ArrayList;
import java.util.List;

public class LibrariesPage extends Page implements KeyUpHandler, ClickHandler {
  public static final String GWT_ID = "org.sonar.plugins.design.ui.libraries.LibrariesPage";

  private Filters filters;
  private List<ProjectPanel> projectPanels = new ArrayList<ProjectPanel>();
  private Panel container;

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    container = new VerticalPanel();
    container.setWidth("100%");

    filters = new Filters(resource);
    filters.getKeywordFilter().addKeyUpHandler(this);
    filters.getTestCheckbox().addClickHandler(this);
    filters.getExpandCollapseLink().addClickHandler(this);
    
    container.add(filters);
    load(resource);
    return container;
  }


  private void load(Resource resource) {
    ResourceQuery resourceQuery = new ResourceQuery(resource.getId().toString());
    resourceQuery.setDepth(-1).setScopes(Resource.SCOPE_SET);
    Sonar.getInstance().findAll(resourceQuery, new AbstractListCallback<Resource>() {
      @Override
      protected void doOnResponse(List<Resource> subProjects) {
        for (Resource subProject : subProjects) {
          ProjectPanel projectPanel = new ProjectPanel(subProject, filters);
          projectPanels.add(projectPanel);
          container.add(projectPanel);
        }
      }
    });
  }

  public void onKeyUp(KeyUpEvent event) {
    for (ProjectPanel projectPanel : projectPanels) {
      projectPanel.filter();
    }
  }

  public void onClick(ClickEvent event) {
    if (event.getSource() == filters.getTestCheckbox()) {
      for (ProjectPanel projectPanel : projectPanels) {
        projectPanel.filter();
      }
    } else if (event.getSource() == filters.getExpandCollapseLink()) {
      if (filters.isExpanded()) {
        filters.collapse();
        expandCollapseLibs(false);
      } else {
        filters.expand();
        expandCollapseLibs(true);
      }
    }
  }

  private void expandCollapseLibs(boolean expand) {
    for (ProjectPanel projectPanel : projectPanels) {
      projectPanel.expandCollapse(expand);
    }
  }
}
