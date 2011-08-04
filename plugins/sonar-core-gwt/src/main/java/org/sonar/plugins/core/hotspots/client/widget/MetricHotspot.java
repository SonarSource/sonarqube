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
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Links;
import org.sonar.wsclient.gwt.AbstractListCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.ArrayList;
import java.util.List;

public class MetricHotspot extends AbstractHotspot {

  private String metric;
  private String title;

  public MetricHotspot(Resource resource, String metric, String title) {
    super(metric + "-hotspot", resource);
    this.metric = metric;
    this.title = title;
  }

  @Override
  Widget createHeader() {
    Dictionary l10n = Dictionary.getDictionary("l10n");
    final Label label = new Label(title);
    label.setStyleName("header");

    final Anchor moreLink = new Anchor(l10n.get("hotspot.moreDetails"));
    moreLink.getElement().setId("more-" + metric);
    moreLink.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.Location.assign(Links.baseUrl() + "/drilldown/measures/" + getResource().getKey() + "?metric=" + metric);
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
        List<HotspotMeasure> measures = new ArrayList<HotspotMeasure>();
        for (Resource resource : resources) {
          for (Measure measure : resource.getMeasures()) {
            measures.add(new HotspotMeasure(resource, measure));
          }
        }

        if (measures.isEmpty()) {
          renderEmptyResults();
          
        } else {
          final Grid grid = new Grid(measures.size(), 3);
          grid.setStyleName("gwt-Hotspot");
          int row = 0;
          HotspotMeasure firstMeasure = measures.get(0);
          for (HotspotMeasure measure : measures) {
            renderNameCell(grid, measure.getResource(), metric, row, 0);
            renderValueCell(grid, measure.getMeasure(), row, 1);
            renderGraphCell(grid, measure.getMeasure(), firstMeasure.getMeasure(), row, 2);
            row++;
          }

          render(grid);
        }
      }
    });
  }

  protected ResourceQuery getResourceQuery() {
    return ResourceQuery.createForResource(getResource(), metric)
        .setScopes(Resource.SCOPE_ENTITY)
        .setDepth(ResourceQuery.DEPTH_UNLIMITED)
        .setLimit(LIMIT);
  }

  public static class HotspotMeasure {
    private Resource resource;
    private Measure measure;

    public HotspotMeasure(Resource resource, Measure measure) {
      super();
      this.resource = resource;
      this.measure = measure;
    }

    public Resource getResource() {
      return resource;
    }

    public Measure getMeasure() {
      return measure;
    }

  }
}
