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
package org.sonar.plugins.core.hotspots.client.widget;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Links;
import org.sonar.plugins.core.hotspots.client.I18nConstants;
import org.sonar.wsclient.gwt.AbstractListCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

public class MostBadlyDesignedFiles extends AbstractHotspot {

  private ListBox metricSelectBox;

  public MostBadlyDesignedFiles(Resource resource) {
    super("design-hotspot", resource);
  }

  @Override
  Widget createHeader() {
    metricSelectBox = new ListBox(false);
    metricSelectBox.addItem(I18nConstants.INSTANCE.lcom4(), "lcom4");
    metricSelectBox.addItem(I18nConstants.INSTANCE.rfc(), "rfc");
    metricSelectBox.setStyleName("small");
    metricSelectBox.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        loadData();
      }
    });

    final Label label = new Label(I18nConstants.INSTANCE.designTitle());
    label.setStyleName("header");

    final Anchor moreLink = new Anchor(I18nConstants.INSTANCE.moreDetails());
    moreLink.getElement().setId("more-design");
    moreLink.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        final String metric = getSelectedMetric();
        Window.Location.assign(Links.baseUrl() + "/drilldown/measures/" + getResource().getId() + "?metric=" + metric);
      }
    });

    final HorizontalPanel horizontal = new HorizontalPanel();
    horizontal.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
    horizontal.setWidth("98%");
    horizontal.add(label);
    horizontal.add(metricSelectBox);
    horizontal.add(moreLink);
    horizontal.setCellHorizontalAlignment(label, HorizontalPanel.ALIGN_LEFT);
    horizontal.setCellHorizontalAlignment(metricSelectBox, HorizontalPanel.ALIGN_LEFT);
    horizontal.setCellHorizontalAlignment(moreLink, HorizontalPanel.ALIGN_RIGHT);

    return horizontal;
  }

  @Override
  void doLoadData() {
    final ResourceQuery query = getResourceQuery();
    Sonar.getInstance().findAll(query, new AbstractListCallback<Resource>() {

      @Override
      protected void doOnResponse(List<Resource> resources) {
        final Grid grid = new Grid(resources.size(), 3);
        grid.setStyleName("gwt-Hotspot");
        int row = 0;
        Measure firstMeasure = null;
        for (Resource resource : resources) {
          if (resource.getMeasures().size() == 1) {
            if (firstMeasure == null) {
              firstMeasure = resource.getMeasures().get(0);
            }
            renderNameCell(grid, resource, firstMeasure.getMetricKey(), row, 0);
            renderValueCell(grid, resource.getMeasures().get(0), row, 1);
            renderGraphCell(grid, resource.getMeasures().get(0), firstMeasure, row, 2);
            row++;
          }
        }

        if (firstMeasure == null) {
          renderEmptyResults();
        } else {
          render(grid);
        }
      }
    });
  }

  public ResourceQuery getResourceQuery() {
    return ResourceQuery.createForResource(getResource(), getSelectedMetric())
        .setDepth(-1)
        .setQualifiers(Resource.QUALIFIER_CLASS)
        .setLimit(LIMIT);
  }

  private String getSelectedMetric() {
    return metricSelectBox.getValue(metricSelectBox.getSelectedIndex());
  }
}

