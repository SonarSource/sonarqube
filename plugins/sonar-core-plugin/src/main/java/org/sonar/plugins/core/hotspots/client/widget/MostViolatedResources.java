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
package org.sonar.plugins.core.hotspots.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Links;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Icons;
import org.sonar.plugins.core.hotspots.client.I18nConstants;
import org.sonar.wsclient.gwt.AbstractListCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;
import java.util.Map;

public class MostViolatedResources extends AbstractHotspot {

  public MostViolatedResources(Resource resource) {
    super("violated-files-hotspot", resource);
  }

  @Override
  Widget createHeader() {
    final Label label = new Label(I18nConstants.INSTANCE.titleMostViolatedResources());
    label.setStyleName("header");

    final Anchor moreLink = new Anchor(I18nConstants.INSTANCE.moreDetails());
    moreLink.getElement().setId("more-violated-resources");
    moreLink.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.Location.assign(Links.baseUrl() + "/drilldown/measures/" + getResource().getId() + "?metric=" + Metrics.WEIGHTED_VIOLATIONS);
      }
    });

    final HorizontalPanel horizontal = new HorizontalPanel();
    horizontal.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
    horizontal.setWidth("98%");
    horizontal.add(label);
    horizontal.add(moreLink);
    horizontal.setCellHorizontalAlignment(label, HorizontalPanel.ALIGN_LEFT);
    horizontal.setCellHorizontalAlignment(moreLink, HorizontalPanel.ALIGN_RIGHT);

    return horizontal;
  }

  @Override
  void doLoadData() {
    final ResourceQuery query = getResourceQuery();
    Sonar.getInstance().findAll(query, new AbstractListCallback<Resource>() {

      @Override
      protected void doOnResponse(List<Resource> resources) {
        Grid grid = new Grid(resources.size(), 11);
        grid.setStyleName("gwt-Hotspot");
        int row = 0;
        for (Resource resource : resources) {
          if (resource.getMeasures().size() > 0) {
            renderNameCell(grid, resource, Metrics.WEIGHTED_VIOLATIONS, row, 0);
            renderPriorities(grid, resource, row);
            row++;
          }
        }
        if (row == 0) {
          renderEmptyResults();
        } else {
          render(grid);
        }
      }
    });
  }


  private void renderPriorities(Grid grid, Resource resource, int row) {
    Measure debt = resource.getMeasures().get(0);
    if (debt != null && debt.getData() != null) {
      Map<String, String> map = debt.getDataAsMap(";");
      renderPriority(grid, row, map, 1, "BLOCKER");
      renderPriority(grid, row, map, 3, "CRITICAL");
      renderPriority(grid, row, map, 5, "MAJOR");
      renderPriority(grid, row, map, 7, "MINOR");
      renderPriority(grid, row, map, 9, "INFO");
    }
  }

  private void renderPriority(Grid grid, int row, Map<String, String> map, int column, String priority) {
    grid.setWidget(row, column, Icons.forPriority(priority).createImage());
    grid.getCellFormatter().setStyleName(row, column, getRowCssClass(row) + " small right");

    if (map.containsKey(priority)) {
      grid.setWidget(row, column + 1, new HTML(map.get(priority)));
    } else {
      grid.setWidget(row, column + 1, new HTML("0"));
    }
    grid.getCellFormatter().setStyleName(row, column + 1, getRowCssClass(row) + " small left");
  }

  private ResourceQuery getResourceQuery() {
    return ResourceQuery.createForResource(getResource(), Metrics.WEIGHTED_VIOLATIONS)
        .setScopes(Resource.SCOPE_ENTITY)
        .setQualifiers(Resource.QUALIFIER_CLASS, Resource.QUALIFIER_FILE, Resource.QUALIFIER_PROJECT)
        .setDepth(ResourceQuery.DEPTH_UNLIMITED)
        .setLimit(LIMIT);
  }
}
