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
package org.sonar.plugins.core.violationsviewer.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.gen2.table.override.client.Grid;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Configuration;
import org.sonar.gwt.Links;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Icons;
import org.sonar.gwt.ui.Loading;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViolationsViewer extends Page {
  public static final String GWT_ID = "org.sonar.plugins.core.violationsviewer.ViolationsViewer";

  private Resource resource;
  private final Panel mainPanel = new VerticalPanel();
  private final Loading loading = new Loading(I18nConstants.INSTANCE.loading());

  // header
  private Grid header = null;
  private ListBox filterBox = null;
  private CheckBox expandCheckbox = null;
  private String defaultFilter;

  // source
  private ViolationsPanel sourcePanel;

  private boolean resourceHasViolations = false;

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    this.resource = resource;
    mainPanel.clear();
    mainPanel.add(loading);
    mainPanel.setWidth("100%");
    mainPanel.setStyleName("gwt-Violations");

    header = new Grid(1, 5);
    header.setWidth("100%");
    header.setStylePrimaryName("gwt-ViewerHeader");
    header.getCellFormatter().setStyleName(0, 0, "thin left");

    initDefaultFilter();
    sourcePanel = new ViolationsPanel(resource, defaultFilter);

    header.setHTML(0, 1, "<div class='cell'><span class='note'>" + I18nConstants.INSTANCE.filter() + "</span></div>");
    header.getCellFormatter().setStyleName(0, 1, "right");

    filterBox = new ListBox();
    filterBox.addItem(I18nConstants.INSTANCE.noFilters(), "");
    filterBox.setStyleName("small");

    filterBox.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        String filter = filterBox.getValue(filterBox.getSelectedIndex());
        loadSources();
        sourcePanel.filter(filter);
        sourcePanel.refresh();
      }
    });

    header.setWidget(0, 2, filterBox);
    header.getCellFormatter().setStyleName(0, 2, "thin cell right");

    header.setHTML(0, 3, "<div class='note'>" + I18nConstants.INSTANCE.expand() + "</div>");
    header.getCellFormatter().setStyleName(0, 3, "thin right");

    expandCheckbox = new CheckBox();
    expandCheckbox.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        loadSources();
        sourcePanel.setExpand(expandCheckbox.getValue());
        sourcePanel.refresh();
      }
    });
    header.setWidget(0, 4, expandCheckbox);
    header.getCellFormatter().setStyleName(0, 4, "thin cell left");

    loadRulePriorities();
    return mainPanel;
  }

  private void initDefaultFilter() {
    defaultFilter = Configuration.getRequestParameter("rule");
    if (defaultFilter == null) {
      defaultFilter = Configuration.getRequestParameter("priority");
    }
  }

  private void loadRulePriorities() {
    final ResourceQuery query = ResourceQuery.createForResource(resource, Metrics.VIOLATIONS)
        .setExcludeRulePriorities(false);
    Sonar.getInstance().find(query, new AbstractCallback<Resource>(loading) {
      @Override
      protected void doOnResponse(Resource resource) {
        setResourceHasViolations(resource);
        displayRulePriorities(resource);
        loadRules(resource);
      }
    });
  }

  private void displayRulePriorities(Resource resource) {
    final Grid grid = new Grid(1, 10);
    header.setWidget(0, 0, grid);

    List<Measure> measures = resource.getMeasures();
    displayRulePriority(grid, 0, "BLOCKER", measures);
    displayRulePriority(grid, 2, "CRITICAL", measures);
    displayRulePriority(grid, 4, "MAJOR", measures);
    displayRulePriority(grid, 6, "MINOR", measures);
    displayRulePriority(grid, 8, "INFO", measures);
  }

  private void displayRulePriority(final Grid grid, final int column, final String priority, final List<Measure> measures) {
    String value = "0";
    for (Measure measure : measures) {
      if (priority.equals(measure.getRulePriority())) {
        value = measure.getFormattedValue();
        filterBox.addItem(priority + " (" + value + ")", priority);
        if (priority.equals(defaultFilter)) {
          filterBox.setSelectedIndex(filterBox.getItemCount() - 1);
        }
        continue;
      }
    }
    grid.setHTML(0, column, Icons.forPriority(priority).getHTML());
    grid.setHTML(0, column + 1, value);
    grid.getCellFormatter().setStyleName(0, column, "thin metric right");
    grid.getCellFormatter().setStyleName(0, column + 1, "thin left value");
  }

  private void loadRules(Resource resource) {
    final ResourceQuery query = ResourceQuery.createForResource(resource, Metrics.VIOLATIONS)
        .setExcludeRules(false);
    Sonar.getInstance().find(query, new AbstractCallback<Resource>(loading) {

      @Override
      protected void doOnResponse(Resource resource) {
        setResourceHasViolations(resource);
        displayRules(resource);
        loadSources();
      }
    });
  }

  private void setResourceHasViolations(Resource resource) {
    resourceHasViolations = resource != null && resource.getMeasure(Metrics.VIOLATIONS) != null;
  }

  private void displayRules(Resource resource) {
    Collections.sort(resource.getMeasures(), new Comparator<Measure>() {
      public int compare(Measure m1, Measure m2) {
        return m1.getRuleName().compareTo(m2.getRuleName());
      }
    });
    filterBox.addItem("", "");
    for (Measure measure : resource.getMeasures()) {
      filterBox.addItem(measure.getRuleName() + " (" + measure.getFormattedValue() + ")", measure.getRuleKey());
      if (measure.getRuleKey().equals(defaultFilter)) {
        filterBox.setSelectedIndex(filterBox.getItemCount() - 1);
      }
    }
    loading.removeFromParent();
    mainPanel.add(header);
  }

  private void loadSources() {
    mainPanel.remove(sourcePanel);
    if (resourceHasViolations || expandCheckbox.getValue()) {
      mainPanel.add(sourcePanel);
    }
  }
}
