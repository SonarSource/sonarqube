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
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Links;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Icons;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

public class MostViolatedRules extends AbstractHotspot {

  private ListBox severity;

  public MostViolatedRules(Resource resource) {
    super("rules-hotspot", resource);
  }

  @Override
  Widget createHeader() {
    Dictionary l10n = Dictionary.getDictionary("l10n");
    severity = new ListBox(false);
    severity.addItem(l10n.get("hotspot.anySeverity"), "");
    severity.addItem("Blocker", "BLOCKER");
    severity.addItem("Critical", "CRITICAL");
    severity.addItem("Major", "MAJOR");
    severity.addItem("Minor", "MINOR");
    severity.addItem("Info", "INFO");
    severity.setStyleName("small");
    severity.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        loadData();
      }
    });

    final Label label = new Label(l10n.get("hotspot.titleMostViolatedRules"));
    label.setStyleName("header");

    final Anchor moreLink = new Anchor(l10n.get("hotspot.moreDetails"));
    moreLink.getElement().setId("more-rules");
    moreLink.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.Location.assign(Links.baseUrl() + "/drilldown/violations/" + getResource().getId());
      }
    });

    final HorizontalPanel horizontal = new HorizontalPanel();
    horizontal.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
    horizontal.setWidth("98%");
    horizontal.add(label);
    horizontal.add(severity);
    horizontal.add(moreLink);
    horizontal.setCellHorizontalAlignment(label, HorizontalPanel.ALIGN_LEFT);
    horizontal.setCellHorizontalAlignment(severity, HorizontalPanel.ALIGN_LEFT);
    horizontal.setCellHorizontalAlignment(moreLink, HorizontalPanel.ALIGN_RIGHT);

    return horizontal;
  }

  @Override
  void doLoadData() {
    final ResourceQuery query = getResourceQuery();
    Sonar.getInstance().find(query, new AbstractCallback<Resource>() {

      @Override
      protected void doOnResponse(Resource resource) {
        if (resource.getMeasures().isEmpty()) {
          renderEmptyResults();
        } else {
          renderGrid(resource);
        }
      }
    });
  }

  private void renderGrid(Resource resource) {
    final Grid grid = new Grid(resource.getMeasures().size(), 4);
    grid.setStyleName("gwt-Hotspot");
    int row = 0;
    Measure firstMeasure = resource.getMeasures().get(0);
    for (Measure measure : resource.getMeasures()) {
      renderRule(grid, measure, row);
      renderValueCell(grid, measure, row, 2);
      renderGraphCell(grid, measure, firstMeasure, row, 3);
      row++;
    }
    render(grid);
  }

  protected void renderRule(final Grid grid, final Measure measure, final int row) {
    Anchor drillDown = new Anchor(measure.getRuleName());
    drillDown.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.Location.assign(Links.baseUrl() + "/drilldown/violations/" + getResource().getId() + "?rule=" + measure.getRuleKey());
      }
    });

    grid.setWidget(row, 0, new HTML("<a id=\"rule" + row + "\" href=\"" + Links.urlForRule(measure.getRuleKey(), false) + "\" onclick=\"window.open(this.href,'rule','height=800,width=900,scrollbars=1,resizable=1');return false;\" title=\"" + measure.getRuleKey() + "\">" + Icons.forPriority(measure.getRulePriority()).getHTML() + "</a>"));
    grid.setWidget(row, 1, drillDown);
    grid.getCellFormatter().setStyleName(row, 0, getRowCssClass(row) + "");
    grid.getCellFormatter().setStyleName(row, 1, getRowCssClass(row) + " resourceCell");
  }

  public ResourceQuery getResourceQuery() {
    ResourceQuery query = ResourceQuery.createForResource(getResource(), Metrics.VIOLATIONS)
        .setDepth(0)
        .setExcludeRules(false)
        .setLimit(LIMIT);
    String priority = getSelectedPriority();
    if (priority!=null) {
      query.setRulePriorities(priority);
    }
    return query;
  }

  private String getSelectedPriority() {
    String priority = severity.getValue(severity.getSelectedIndex());
    if ("".equals(priority) || priority == null) {
      return null;
    }
    return priority;
  }
}
