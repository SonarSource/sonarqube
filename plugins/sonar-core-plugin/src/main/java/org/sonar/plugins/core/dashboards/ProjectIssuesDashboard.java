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
package org.sonar.plugins.core.dashboards;

import com.google.common.base.Preconditions;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.plugins.core.widgets.issues.ProjectIssueFilterWidget;

/**
 * Issues dashboard for Sonar
 *
 * @since 3.6
 */
public final class ProjectIssuesDashboard extends DashboardTemplate {

  private final IssueFilterDao issueFilterDao;

  public ProjectIssuesDashboard(IssueFilterDao issueFilterDao) {
    this.issueFilterDao = issueFilterDao;
  }

  @Override
  public String getName() {
    return "Issues";
  }

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.create();
    dashboard.setLayout(DashboardLayout.TWO_COLUMNS);

    IssueFilterDto unresolvedIssues = getIssueFilterByName("Unresolved Issues");
    IssueFilterDto hiddenDebt = getIssueFilterByName("False Positive and Won't Fix Issues");
    IssueFilterDto myUnresolvedIssues = getIssueFilterByName("My Unresolved Issues");

    addFirstColumn(dashboard, unresolvedIssues);
    addSecondColumn(dashboard, unresolvedIssues, hiddenDebt, myUnresolvedIssues);
    return dashboard;
  }

  private IssueFilterDto getIssueFilterByName(String name) {
    IssueFilterDto filter = issueFilterDao.selectProvidedFilterByName(name);
    Preconditions.checkState(filter != null, String.format("Could not find a provided issue filter with name '%s'", name));
    return filter;
  }

  private void addFirstColumn(Dashboard dashboard, IssueFilterDto unresolvedIssues) {
    // Unresolved issues by status
    dashboard.addWidget(ProjectIssueFilterWidget.ID, 1)
      .setProperty(ProjectIssueFilterWidget.FILTER_PROPERTY, Long.toString(unresolvedIssues.getId()))
      .setProperty(ProjectIssueFilterWidget.DISTRIBUTION_AXIS_PROPERTY, "statuses");
    // Action plans
    dashboard.addWidget("action_plans", 1);
  }

  private void addSecondColumn(Dashboard dashboard, IssueFilterDto unresolvedIssues, IssueFilterDto hiddenDebt, IssueFilterDto myUnresolvedIssues) {
    // Unresolved issues by assignee
    dashboard.addWidget(ProjectIssueFilterWidget.ID, 2)
      .setProperty(ProjectIssueFilterWidget.FILTER_PROPERTY, Long.toString(unresolvedIssues.getId()))
      .setProperty(ProjectIssueFilterWidget.DISTRIBUTION_AXIS_PROPERTY, "assignees");
    // My unresolved issues
    dashboard.addWidget(ProjectIssueFilterWidget.ID, 2)
      .setProperty("filter", Long.toString(myUnresolvedIssues.getId()));
    // False positive and won't fix issues by resolution
    dashboard.addWidget(ProjectIssueFilterWidget.ID, 2)
      .setProperty(ProjectIssueFilterWidget.FILTER_PROPERTY, Long.toString(hiddenDebt.getId()))
      .setProperty(ProjectIssueFilterWidget.DISTRIBUTION_AXIS_PROPERTY, "resolutions");
  }

}
