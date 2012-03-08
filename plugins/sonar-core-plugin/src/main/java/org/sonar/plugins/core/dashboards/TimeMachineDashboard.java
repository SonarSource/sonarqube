/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.core.dashboards;

import org.sonar.api.web.Dashboard;
import org.sonar.api.web.Dashboard.Widget;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

/**
 * Time Machine dashboard for Sonar
 *
 * @since 2.15
 */
public final class TimeMachineDashboard extends DashboardTemplate {

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
    timelineWidget.setProperty("metric1", "complexity");
    timelineWidget.setProperty("metric2", "violations_density");
    timelineWidget.setProperty("metric3", "coverage");
    
    Widget sizeTimeMachineWidget = dashboard.addWidget("time_machine", 1);
    sizeTimeMachineWidget.setProperty("displaySparkLine", "true");
    sizeTimeMachineWidget.setProperty("metric1", "ncloc");
    sizeTimeMachineWidget.setProperty("metric2", "lines");
    sizeTimeMachineWidget.setProperty("metric3", "statements");
    sizeTimeMachineWidget.setProperty("metric4", "files");
    sizeTimeMachineWidget.setProperty("metric5", "classes");
    sizeTimeMachineWidget.setProperty("metric6", "functions");
    sizeTimeMachineWidget.setProperty("metric7", "accessors");
    
    Widget commentsTimeMachineWidget = dashboard.addWidget("time_machine", 1);
    commentsTimeMachineWidget.setProperty("displaySparkLine", "true");
    commentsTimeMachineWidget.setProperty("metric1", "comment_lines_density");
    commentsTimeMachineWidget.setProperty("metric2", "comment_lines");
    commentsTimeMachineWidget.setProperty("metric3", "public_documented_api_density");
    commentsTimeMachineWidget.setProperty("metric4", "public_undocumented_api");
    
    Widget duplicationTimeMachineWidget = dashboard.addWidget("time_machine", 1);
    duplicationTimeMachineWidget.setProperty("displaySparkLine", "true");
    duplicationTimeMachineWidget.setProperty("metric1", "duplicated_lines_density");
    duplicationTimeMachineWidget.setProperty("metric2", "duplicated_lines");
    duplicationTimeMachineWidget.setProperty("metric3", "duplicated_blocks");
    duplicationTimeMachineWidget.setProperty("metric4", "duplicated_files");
  }

  private void addSecondColumn(Dashboard dashboard) {
    Widget rulesTimeMachineWidget = dashboard.addWidget("time_machine", 2);
    rulesTimeMachineWidget.setProperty("displaySparkLine", "true");
    rulesTimeMachineWidget.setProperty("metric1", "violations");
    rulesTimeMachineWidget.setProperty("metric2", "violation_density");
    rulesTimeMachineWidget.setProperty("metric3", "blocker_violations");
    rulesTimeMachineWidget.setProperty("metric4", "critical_violations");
    rulesTimeMachineWidget.setProperty("metric5", "major_violations");
    rulesTimeMachineWidget.setProperty("metric6", "minor_violations");
    rulesTimeMachineWidget.setProperty("metric7", "info_violations");
    rulesTimeMachineWidget.setProperty("metric7", "weighted_violations");

    Widget complexityTimeMachineWidget = dashboard.addWidget("time_machine", 2);
    complexityTimeMachineWidget.setProperty("displaySparkLine", "true");
    complexityTimeMachineWidget.setProperty("metric1", "complexity");
    complexityTimeMachineWidget.setProperty("metric2", "function_complexity");
    complexityTimeMachineWidget.setProperty("metric3", "class_complexity");
    complexityTimeMachineWidget.setProperty("metric4", "file_complexity");

    Widget testsTimeMachineWidget = dashboard.addWidget("time_machine", 2);
    testsTimeMachineWidget.setProperty("displaySparkLine", "true");
    testsTimeMachineWidget.setProperty("metric1", "coverage");
    testsTimeMachineWidget.setProperty("metric2", "line_coverage");
    testsTimeMachineWidget.setProperty("metric3", "branch_coverage");
    testsTimeMachineWidget.setProperty("metric4", "test_success_density");
    testsTimeMachineWidget.setProperty("metric5", "test_failures");
    testsTimeMachineWidget.setProperty("metric6", "test_errors");
    testsTimeMachineWidget.setProperty("metric7", "tests");
    testsTimeMachineWidget.setProperty("metric7", "test_execution_time");
  }

}