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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Links;
import org.sonar.gwt.ui.Loading;
import org.sonar.plugins.core.hotspots.client.I18nConstants;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;

public abstract class AbstractHotspot extends Composite {

  private Panel hotspot;
  private Panel data;
  private Resource resource;

  public static final int LIMIT = 5;

  protected AbstractHotspot(String id, Resource resource) {
    this.resource = resource;
    hotspot = new VerticalPanel();
    hotspot.getElement().setId(id);
    hotspot.setStyleName("gwt-HotspotPanel");
    initWidget(hotspot);
  }

  public Resource getResource() {
    return resource;
  }

  @Override
  public void onLoad() {
    hotspot.add(createHeader());
    data = new SimplePanel();
    hotspot.add(data);
    loadData();
  }

  protected void loadData() {
    data.clear();
    data.add(new Loading());
    doLoadData();
  }

  abstract Widget createHeader();

  abstract void doLoadData();

  protected void render(Widget widget) {
    data.clear();
    data.add(widget);
  }

  protected void renderEmptyResults() {
    Grid grid = new Grid(1, 1);
    grid.setWidget(0, 0, new HTML(I18nConstants.INSTANCE.noMeasures()));
    grid.getCellFormatter().setStyleName(0, 0, getRowCssClass(0) + " emptyResultsCell");
    grid.setStyleName("gwt-Hotspot");
    render(grid);
  }

  protected void renderNameCell(Grid hotspotGrid, final Resource resource, final String metricKey, int row, int column) {
    Anchor link = new Anchor(resource.getName());
    link.getElement().setAttribute("title", resource.getName(true));
    link.getElement().setAttribute("rel", resource.getName(true));
    link.addClickHandler(new ClickHandler() {
      public void onClick(final ClickEvent event) {
        if (resource.getCopy() != null) {
          Window.Location.assign(Links.baseUrl() + "/plugins/resource/" + resource.getCopy() + "?page=org.sonar.plugins.core.hotspots.GwtHotspots");
        } else {
          Links.openMeasurePopup(resource.getKey(), metricKey);
        }
      }
    });
    hotspotGrid.setWidget(row, column, link);
    hotspotGrid.getCellFormatter().setStyleName(row, column, getRowCssClass(row) + " resourceCell");
  }

  protected void renderValueCell(Grid hotspotGrid, Measure measure, int row, int column) {
    hotspotGrid.setHTML(row, column, measure.getFormattedValue());
    hotspotGrid.getCellFormatter().setStyleName(row, column, getRowCssClass(row) + " resultCell");
  }

  protected void renderGraphCell(Grid hotspotGrid, Measure measure, Measure firstMeasure, int row, int column) {
    Double value = Double.valueOf(measure.getValue());
    Double upperValue = Double.valueOf(firstMeasure.getValue());
    Double percentPonderated = getPercentPonderatedValue(value, 0d, upperValue);
    String graph = "<span style='width:100%'><ul class='hbar' style='float: right;'><li style='background-color: rgb(119, 119, 119); width: " + percentPonderated.intValue() + "%'>&nbsp;</li></ul></span>";
    hotspotGrid.setHTML(row, column, graph);
    hotspotGrid.getCellFormatter().setStyleName(row, column, getRowCssClass(row) + " graphCell");
  }

  protected String getRowCssClass(int row) {
    return row % 2 == 0 ? "even" : "odd";
  }

  protected double getPercentPonderatedValue(Double value, Double lower, Double upper) {
    if (value < lower) return 0;
    if (value > upper) return 100;
    double percentIncrement = (upper - lower) / 100d;
    return (value - lower) / percentIncrement;
  }
}
