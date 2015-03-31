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
package org.sonar.server.startup;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.picocontainer.Startable;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.dashboard.DashboardDto;
import org.sonar.core.dashboard.WidgetDto;
import org.sonar.core.dashboard.WidgetPropertyDto;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.filter.RegisterIssueFilters;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class RenameIssueWidgets implements Startable {

  private static final Logger LOGGER = Loggers.get(RenameIssueWidgets.class);

  private static final String TASK_KEY = "RenameIssueWidgets";

  private static final String WIDGET_FALSE_POSITIVES = "false_positive_reviews";
  private static final String WIDGET_MY_UNRESOLVED = "my_reviews";
  private static final String WIDGET_UNRESOLVED_BY_DEVELOPER = "reviews_per_developer";
  private static final String WIDGET_UNRESOLVED_BY_STATUS = "unresolved_issues_statuses";

  private static final String WIDGET_ISSUE_FILTER = "issue_filter";
  private static final String WIDGET_PROJECT_ISSUE_FILTER = "project_issue_filter";

  private static final String FILTER_PROPERTY = "filter";
  private static final String DISTRIBUTION_AXIS_PROPERTY = "distributionAxis";

  private final DbClient dbClient;

  private final System2 system;

  public RenameIssueWidgets(DbClient dbClient, System2 system, RegisterIssueFilters startupDependency) {
    this.dbClient = dbClient;
    this.system = system;
    // RegisterIssueFilters must be run before this task, to be able to reference issue filters in widget properties
  }

  @Override
  public void start() {
    DbSession session = dbClient.openSession(false);

    try {
      if (dbClient.loadedTemplateDao().countByTypeAndKey(LoadedTemplateDto.ONE_SHOT_TASK_TYPE, TASK_KEY, session) != 0) {
        // Already done
        return;
      }

      Map<String, IssueFilterDto> filterByWidgetKey = loadRequiredIssueFilters();

      Map<String, String> distributionAxisByWidgetKey = ImmutableMap.of(
        WIDGET_FALSE_POSITIVES, "resolutions",
        WIDGET_MY_UNRESOLVED, "severities",
        WIDGET_UNRESOLVED_BY_DEVELOPER, "assignees",
        WIDGET_UNRESOLVED_BY_STATUS, "statuses"
      );

      LOGGER.info("Replacing issue related widgets with issue filter widgets");

      List<Long> updatedWidgetIds = Lists.newArrayList();
      List<WidgetPropertyDto> newWidgetProperties = Lists.newArrayList();

      for (WidgetDto widget : dbClient.widgetDao().findAll(session)) {
        String widgetKey = widget.getWidgetKey();
        if (filterByWidgetKey.keySet().contains(widgetKey)) {
          updatedWidgetIds.add(widget.getId());
          newWidgetProperties.add(createFilterProperty(filterByWidgetKey.get(widgetKey), widget));
          newWidgetProperties.add(createDistributionAxisProperty(distributionAxisByWidgetKey.get(widgetKey), widget));
          updateWidget(session, widget);
        }
      }

      dbClient.widgetPropertyDao().deleteByWidgetIds(session, updatedWidgetIds);
      dbClient.widgetPropertyDao().insert(session, newWidgetProperties);

      dbClient.loadedTemplateDao().insert(new LoadedTemplateDto()
        .setType(LoadedTemplateDto.ONE_SHOT_TASK_TYPE)
        .setKey(TASK_KEY), session);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  protected Map<String, IssueFilterDto> loadRequiredIssueFilters() {
    IssueFilterDto unresolvedIssues = dbClient.issueFilterDao().selectProvidedFilterByName("Unresolved Issues");
    IssueFilterDto hiddenDebt = dbClient.issueFilterDao().selectProvidedFilterByName("False Positive and Won't Fix Issues");
    IssueFilterDto myUnresolvedIssues = dbClient.issueFilterDao().selectProvidedFilterByName("My Unresolved Issues");

    return ImmutableMap.of(
      WIDGET_FALSE_POSITIVES, hiddenDebt,
      WIDGET_MY_UNRESOLVED, myUnresolvedIssues,
      WIDGET_UNRESOLVED_BY_DEVELOPER, unresolvedIssues,
      WIDGET_UNRESOLVED_BY_STATUS, unresolvedIssues
    );
  }

  private WidgetPropertyDto createFilterProperty(IssueFilterDto issueFilter, WidgetDto widget) {
    return createWidgetProperty(FILTER_PROPERTY, issueFilter.getId().toString(), widget);
  }

  private WidgetPropertyDto createDistributionAxisProperty(String distributionAxis, WidgetDto widget) {
    return createWidgetProperty(DISTRIBUTION_AXIS_PROPERTY, distributionAxis, widget);
  }

  private WidgetPropertyDto createWidgetProperty(String key, String value, WidgetDto widget) {
    return new WidgetPropertyDto()
      .setWidgetId(widget.getId())
      .setPropertyKey(key)
      .setTextValue(value);
  }

  private void updateWidget(DbSession session, WidgetDto widget) {
    dbClient.widgetDao().update(session,
      widget.setWidgetKey(getReplacementWidgetKey(session, widget))
        .setUpdatedAt(new Date(system.now()))
          .setConfigured(true));
  }

  private String getReplacementWidgetKey(DbSession session, WidgetDto widget) {
    DashboardDto dashboard = dbClient.dashboardDao().getNullableByKey(session, widget.getDashboardId());
    if (dashboard == null) {
      LOGGER.warn(String.format("Widget with ID=%d is not displayed on any dashboard, updating nevertheless", widget.getId()));
    }
    boolean isOnGlobalDashboard = dashboard != null && dashboard.getGlobal();

    return isOnGlobalDashboard && widget.getResourceId() == null ? WIDGET_ISSUE_FILTER : WIDGET_PROJECT_ISSUE_FILTER;
  }

  @Override
  public void stop() {
    // do nothing
  }

}
