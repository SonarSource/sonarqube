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
import org.sonar.gwt.JsonUtils;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Icons;
import org.sonar.gwt.ui.Loading;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.*;

public class ViolationsViewer extends Page {
  public static final String GWT_ID = "org.sonar.plugins.core.violationsviewer.ViolationsViewer";

  private Resource resource;
  private final Panel mainPanel = new VerticalPanel();
  private final Loading loading = new Loading(I18nConstants.INSTANCE.loading());

  // header
  private Grid header = null;
  private ListBox filterBox = null, periodBox = null;
  private List<Date> dateFilters = new ArrayList<Date>();
  private CheckBox expandCheckbox = null;

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


    header.getCellFormatter().setStyleName(0, 1, "right");

    initFilters();

    if (periodBox.getItemCount() > 1) {
      header.setWidget(0, 1, periodBox);
      header.getCellFormatter().setStyleName(0, 1, "thin cell right");
    }

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

    loadRuleSeverities();
    return mainPanel;
  }

  private void initFilters() {
    initFilterBox();
    initPeriodBox();

    ChangeHandler changeHandler = new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        loadSources();
        sourcePanel.filter(getCurrentRuleFilter(), getCurrentDateFilter());
        sourcePanel.refresh();
      }
    };
    filterBox.addChangeHandler(changeHandler);
    periodBox.addChangeHandler(changeHandler);
  }

  private void initFilterBox() {
    filterBox = new ListBox();
    filterBox.addItem(I18nConstants.INSTANCE.noFilters(), "");
    filterBox.setStyleName("small");
  }

  private void initPeriodBox() {
    periodBox = new ListBox();
    periodBox.setStyleName("small");
    periodBox.addItem(I18nConstants.INSTANCE.addedPeriod());

    initPeriod(1);
    initPeriod(2);
    initPeriod(3);
    initPeriod(4);
    initPeriod(5);

    String period = Configuration.getRequestParameter("period");
    if (period != null && !"".equals(period)) {
      periodBox.setSelectedIndex(Integer.valueOf(period));
    }
  }

  private void initPeriod(int periodIndex) {
    String period = Configuration.getParameter("period" + periodIndex);
    if (period != null) {
      Date date = JsonUtils.parseDateTime(Configuration.getParameter("period" + periodIndex + "_date"));
      if (date != null) {
        periodBox.addItem("Added " + period);
        dateFilters.add(date);
      }
    }
  }

  private Date getCurrentDateFilter() {
    Date dateFilter = null;
    if (periodBox.getSelectedIndex() > 0) {
      dateFilter = dateFilters.get(periodBox.getSelectedIndex() - 1);
    }
    return dateFilter;
  }

  private String getCurrentRuleFilter() {
    return filterBox.getValue(filterBox.getSelectedIndex());
  }

  private void loadRuleSeverities() {
    final ResourceQuery query = ResourceQuery.createForResource(resource, Metrics.BLOCKER_VIOLATIONS,
        Metrics.CRITICAL_VIOLATIONS, Metrics.MAJOR_VIOLATIONS, Metrics.MINOR_VIOLATIONS, Metrics.INFO_VIOLATIONS);
    Sonar.getInstance().find(query, new AbstractCallback<Resource>(loading) {
      @Override
      protected void doOnResponse(Resource resource) {
        String defaultFilter = Configuration.getRequestParameter("rule");
        if (defaultFilter == null) {
          defaultFilter = Configuration.getRequestParameter("priority");
        }
        setResourceHasViolations(resource);
        displayRuleSeverities(resource, defaultFilter);
        loadRules(resource, defaultFilter);
      }
    });
  }

  private void displayRuleSeverities(Resource resource, String defaultFilter) {
    final Grid grid = new Grid(1, 10);
    header.setWidget(0, 0, grid);

    displayRuleSeverity(grid, 0, "BLOCKER", defaultFilter, resource.getMeasure(Metrics.BLOCKER_VIOLATIONS));
    displayRuleSeverity(grid, 2, "CRITICAL", defaultFilter, resource.getMeasure(Metrics.CRITICAL_VIOLATIONS));
    displayRuleSeverity(grid, 4, "MAJOR", defaultFilter, resource.getMeasure(Metrics.MAJOR_VIOLATIONS));
    displayRuleSeverity(grid, 6, "MINOR", defaultFilter, resource.getMeasure(Metrics.MINOR_VIOLATIONS));
    displayRuleSeverity(grid, 8, "INFO", defaultFilter, resource.getMeasure(Metrics.INFO_VIOLATIONS));
  }

  private void displayRuleSeverity(final Grid grid, final int column, final String severity, final String defaultFilter, final Measure measure) {
    String value = "0";
    if (measure != null) {
      value = measure.getFormattedValue();
      filterBox.addItem(severity + " (" + value + ")", severity);
      if (severity.equals(defaultFilter)) {
        filterBox.setSelectedIndex(filterBox.getItemCount() - 1);
      }
    }

    grid.setHTML(0, column, Icons.forSeverity(severity).getHTML());
    grid.setHTML(0, column + 1, value);
    grid.getCellFormatter().setStyleName(0, column, "thin metric right");
    grid.getCellFormatter().setStyleName(0, column + 1, "thin left value");
  }

  private void loadRules(final Resource resource, final String defaultFilter) {
    final ResourceQuery query = ResourceQuery.createForResource(resource, Metrics.VIOLATIONS)
        .setExcludeRules(false);
    Sonar.getInstance().find(query, new AbstractCallback<Resource>(loading) {

      @Override
      protected void doOnResponse(Resource resource) {
        setResourceHasViolations(resource);
        displayRules(resource, defaultFilter);
        loadSources();
      }
    });
  }

  private void setResourceHasViolations(Resource resource) {
    resourceHasViolations = resource != null && resource.getMeasure(Metrics.VIOLATIONS) != null;
  }

  private void displayRules(final Resource resource, final String defaultFilter) {
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
    if (sourcePanel == null) {
      sourcePanel = new ViolationsPanel(resource, getCurrentRuleFilter(), getCurrentDateFilter());
      mainPanel.add(sourcePanel);
    } else {
      mainPanel.remove(sourcePanel);
      if (resourceHasViolations || expandCheckbox.getValue()) {
        mainPanel.add(sourcePanel);
      }
    }
  }
}
