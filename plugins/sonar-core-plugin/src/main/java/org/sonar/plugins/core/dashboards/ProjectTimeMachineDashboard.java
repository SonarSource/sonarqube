/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.core.dashboards;

import org.sonar.api.web.Dashboard;
import org.sonar.api.web.Dashboard.Widget;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

/**
 * Time Machine dashboard for Sonar
 *
 * @since 3.0
 */
public final class ProjectTimeMachineDashboard extends DashboardTemplate {

  private static final String METRIC1 = "metric1";
  private static final String METRIC2 = "metric2";
  private static final String METRIC3 = "metric3";
  private static final String METRIC4 = "metric4";
  private static final String METRIC5 = "metric5";
  private static final String METRIC6 = "metric6";
  private static final String METRIC7 = "metric7";
  private static final String COVERAGE = "coverage";

  @Override
  public String getName() {
    return "TimeMachine";
  }

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.create();
    dashboard.setLayout(DashboardLayout.TWO_COLUMNS);
    addFirstColumn(dashboard);
    addSecondColumn(dashboard);
    return dashboard;
  }

  private void addFirstColumn(Dashboard dashboard) {
    Widget timelineWidget = dashboard.addWidget("timeline", 1);
    timelineWidget.setProperty(METRIC1, "complexity");
    timelineWidget.setProperty(METRIC2, "violations_density");
    timelineWidget.setProperty(METRIC3, COVERAGE);

    Widget sizeTimeMachineWidget = addTimeMachineWidgetOnFirstColumn(dashboard);
    sizeTimeMachineWidget.setProperty(METRIC1, "ncloc");
    sizeTimeMachineWidget.setProperty(METRIC2, "lines");
    sizeTimeMachineWidget.setProperty(METRIC3, "statements");
    sizeTimeMachineWidget.setProperty(METRIC4, "files");
    sizeTimeMachineWidget.setProperty(METRIC5, "classes");
    sizeTimeMachineWidget.setProperty(METRIC6, "functions");
    sizeTimeMachineWidget.setProperty(METRIC7, "accessors");

    Widget commentsTimeMachineWidget = addTimeMachineWidgetOnFirstColumn(dashboard);
    commentsTimeMachineWidget.setProperty(METRIC1, "comment_lines_density");
    commentsTimeMachineWidget.setProperty(METRIC2, "comment_lines");
    commentsTimeMachineWidget.setProperty(METRIC3, "public_documented_api_density");
    commentsTimeMachineWidget.setProperty(METRIC4, "public_undocumented_api");

    Widget duplicationTimeMachineWidget = addTimeMachineWidgetOnFirstColumn(dashboard);
    duplicationTimeMachineWidget.setProperty(METRIC1, "duplicated_lines_density");
    duplicationTimeMachineWidget.setProperty(METRIC2, "duplicated_lines");
    duplicationTimeMachineWidget.setProperty(METRIC3, "duplicated_blocks");
    duplicationTimeMachineWidget.setProperty(METRIC4, "duplicated_files");
  }

  private void addSecondColumn(Dashboard dashboard) {
    Widget rulesTimeMachineWidget = addTimeMachineWidgetOnSecondColumn(dashboard);
    rulesTimeMachineWidget.setProperty(METRIC1, "violations");
    rulesTimeMachineWidget.setProperty(METRIC2, "violation_density");
    rulesTimeMachineWidget.setProperty(METRIC3, "blocker_violations");
    rulesTimeMachineWidget.setProperty(METRIC4, "critical_violations");
    rulesTimeMachineWidget.setProperty(METRIC5, "major_violations");
    rulesTimeMachineWidget.setProperty(METRIC6, "minor_violations");
    rulesTimeMachineWidget.setProperty(METRIC7, "info_violations");
    rulesTimeMachineWidget.setProperty(METRIC7, "weighted_violations");

    Widget complexityTimeMachineWidget = addTimeMachineWidgetOnSecondColumn(dashboard);
    complexityTimeMachineWidget.setProperty(METRIC1, "complexity");
    complexityTimeMachineWidget.setProperty(METRIC2, "function_complexity");
    complexityTimeMachineWidget.setProperty(METRIC3, "class_complexity");
    complexityTimeMachineWidget.setProperty(METRIC4, "file_complexity");

    Widget testsTimeMachineWidget = addTimeMachineWidgetOnSecondColumn(dashboard);
    testsTimeMachineWidget.setProperty(METRIC1, COVERAGE);
    testsTimeMachineWidget.setProperty(METRIC2, "line_coverage");
    testsTimeMachineWidget.setProperty(METRIC3, "branch_coverage");
    testsTimeMachineWidget.setProperty(METRIC4, "test_success_density");
    testsTimeMachineWidget.setProperty(METRIC5, "test_failures");
    testsTimeMachineWidget.setProperty(METRIC6, "test_errors");
    testsTimeMachineWidget.setProperty(METRIC7, "tests");
    testsTimeMachineWidget.setProperty(METRIC7, "test_execution_time");
  }

  private Widget addTimeMachineWidgetOnFirstColumn(Dashboard dashboard) {
    return addTimeMachineWidget(dashboard, 1);
  }

  private Widget addTimeMachineWidgetOnSecondColumn(Dashboard dashboard) {
    return addTimeMachineWidget(dashboard, 2);
  }

  private Widget addTimeMachineWidget(Dashboard dashboard, int columnIndex) {
    Widget widget = dashboard.addWidget("time_machine", columnIndex);
    widget.setProperty("displaySparkLine", "true");
    return widget;
  }

}
