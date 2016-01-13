/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.dashboard.template;

import com.google.common.base.Preconditions;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;
import org.sonar.db.issue.IssueFilterDao;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.server.dashboard.widget.ProjectIssueFilterWidget;

/**
 * Custom dashboard
 *
 * @since 2.13
 */
public final class ProjectCustomDashboard extends DashboardTemplate {

  private final IssueFilterDao issueFilterDao;

  public ProjectCustomDashboard(IssueFilterDao issueFilterDao) {
    this.issueFilterDao = issueFilterDao;
  }

  @Override
  public String getName() {
    return "Custom";
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
    dashboard.addWidget("size", 1);
    dashboard.addWidget("code_coverage", 1);
    dashboard.addWidget("duplications", 1);
    dashboard.addWidget("documentation_comments", 1);
  }

  private void addSecondColumn(Dashboard dashboard) {
    dashboard.addWidget("rules", 2);
    dashboard.addWidget("timeline", 2);
    IssueFilterDto unresolvedIssues = getIssueFilterByName("Unresolved Issues");
    dashboard.addWidget(ProjectIssueFilterWidget.ID, 2)
      .setProperty(ProjectIssueFilterWidget.FILTER_PROPERTY, Long.toString(unresolvedIssues.getId()))
      .setProperty(ProjectIssueFilterWidget.DISTRIBUTION_AXIS_PROPERTY, "createdAt");
  }

  private IssueFilterDto getIssueFilterByName(String name) {
    IssueFilterDto filter = issueFilterDao.selectProvidedFilterByName(name);
    Preconditions.checkState(filter != null, String.format("Could not find a provided issue filter with name '%s'", name));
    return filter;
  }

}
