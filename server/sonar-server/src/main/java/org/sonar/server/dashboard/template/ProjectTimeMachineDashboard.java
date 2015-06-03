/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.dashboard.template;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.Dashboard.Widget;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

/**
 * Time Machine dashboard
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
  private static final String METRIC8 = "metric8";

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

  private static void addFirstColumn(Dashboard dashboard) {
    Widget timelineWidget = dashboard.addWidget("timeline", 1);
    timelineWidget.setProperty(METRIC1, CoreMetrics.COMPLEXITY_KEY);
    timelineWidget.setProperty(METRIC2, CoreMetrics.TECHNICAL_DEBT_KEY);
    timelineWidget.setProperty(METRIC3, CoreMetrics.COVERAGE_KEY);

    Widget sizeTimeMachineWidget = addTimeMachineWidgetOnFirstColumn(dashboard);
    sizeTimeMachineWidget.setProperty(METRIC1, CoreMetrics.NCLOC_KEY);
    sizeTimeMachineWidget.setProperty(METRIC2, CoreMetrics.LINES_KEY);
    sizeTimeMachineWidget.setProperty(METRIC3, CoreMetrics.STATEMENTS_KEY);
    sizeTimeMachineWidget.setProperty(METRIC4, CoreMetrics.FILES_KEY);
    sizeTimeMachineWidget.setProperty(METRIC5, CoreMetrics.CLASSES_KEY);
    sizeTimeMachineWidget.setProperty(METRIC6, CoreMetrics.FUNCTIONS_KEY);
    sizeTimeMachineWidget.setProperty(METRIC7, CoreMetrics.ACCESSORS_KEY);

    Widget commentsTimeMachineWidget = addTimeMachineWidgetOnFirstColumn(dashboard);
    commentsTimeMachineWidget.setProperty(METRIC1, CoreMetrics.COMMENT_LINES_DENSITY_KEY);
    commentsTimeMachineWidget.setProperty(METRIC2, CoreMetrics.COMMENT_LINES_KEY);
    commentsTimeMachineWidget.setProperty(METRIC3, CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY_KEY);
    commentsTimeMachineWidget.setProperty(METRIC4, CoreMetrics.PUBLIC_UNDOCUMENTED_API_KEY);

    Widget duplicationTimeMachineWidget = addTimeMachineWidgetOnFirstColumn(dashboard);
    duplicationTimeMachineWidget.setProperty(METRIC1, CoreMetrics.DUPLICATED_LINES_DENSITY_KEY);
    duplicationTimeMachineWidget.setProperty(METRIC2, CoreMetrics.DUPLICATED_LINES_KEY);
    duplicationTimeMachineWidget.setProperty(METRIC3, CoreMetrics.DUPLICATED_BLOCKS_KEY);
    duplicationTimeMachineWidget.setProperty(METRIC4, CoreMetrics.DUPLICATED_FILES_KEY);
  }

  private static void addSecondColumn(Dashboard dashboard) {
    Widget rulesTimeMachineWidget = addTimeMachineWidgetOnSecondColumn(dashboard);
    rulesTimeMachineWidget.setProperty(METRIC1, CoreMetrics.VIOLATIONS_KEY);
    rulesTimeMachineWidget.setProperty(METRIC2, CoreMetrics.BLOCKER_VIOLATIONS_KEY);
    rulesTimeMachineWidget.setProperty(METRIC3, CoreMetrics.CRITICAL_VIOLATIONS_KEY);
    rulesTimeMachineWidget.setProperty(METRIC4, CoreMetrics.MAJOR_VIOLATIONS_KEY);
    rulesTimeMachineWidget.setProperty(METRIC5, CoreMetrics.MINOR_VIOLATIONS_KEY);
    rulesTimeMachineWidget.setProperty(METRIC6, CoreMetrics.INFO_VIOLATIONS_KEY);
    rulesTimeMachineWidget.setProperty(METRIC7, CoreMetrics.TECHNICAL_DEBT_KEY);

    Widget complexityTimeMachineWidget = addTimeMachineWidgetOnSecondColumn(dashboard);
    complexityTimeMachineWidget.setProperty(METRIC1, CoreMetrics.COMPLEXITY_KEY);
    complexityTimeMachineWidget.setProperty(METRIC2, CoreMetrics.FUNCTION_COMPLEXITY_KEY);
    complexityTimeMachineWidget.setProperty(METRIC3, CoreMetrics.CLASS_COMPLEXITY_KEY);
    complexityTimeMachineWidget.setProperty(METRIC4, CoreMetrics.FILE_COMPLEXITY_KEY);

    Widget testsTimeMachineWidget = addTimeMachineWidgetOnSecondColumn(dashboard);
    testsTimeMachineWidget.setProperty(METRIC1, CoreMetrics.COVERAGE_KEY);
    testsTimeMachineWidget.setProperty(METRIC2, CoreMetrics.LINE_COVERAGE_KEY);
    testsTimeMachineWidget.setProperty(METRIC3, CoreMetrics.BRANCH_COVERAGE_KEY);
    testsTimeMachineWidget.setProperty(METRIC4, CoreMetrics.TEST_SUCCESS_DENSITY_KEY);
    testsTimeMachineWidget.setProperty(METRIC5, CoreMetrics.TEST_FAILURES_KEY);
    testsTimeMachineWidget.setProperty(METRIC6, CoreMetrics.TEST_ERRORS_KEY);
    testsTimeMachineWidget.setProperty(METRIC7, CoreMetrics.TESTS_KEY);
    testsTimeMachineWidget.setProperty(METRIC8, CoreMetrics.TEST_EXECUTION_TIME_KEY);
  }

  private static Widget addTimeMachineWidgetOnFirstColumn(Dashboard dashboard) {
    return addTimeMachineWidget(dashboard, 1);
  }

  private static Widget addTimeMachineWidgetOnSecondColumn(Dashboard dashboard) {
    return addTimeMachineWidget(dashboard, 2);
  }

  private static Widget addTimeMachineWidget(Dashboard dashboard, int columnIndex) {
    Widget widget = dashboard.addWidget("time_machine", columnIndex);
    widget.setProperty("displaySparkLine", "true");
    return widget;
  }

}
