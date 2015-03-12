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

import com.google.common.collect.Lists;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.dashboard.WidgetDto;
import org.sonar.core.dashboard.WidgetPropertyDto;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.server.db.DbClient;

import java.util.List;

public class RenameIssueWidgets implements Startable {

  private static final String PROJECT_ISSUE_FILTER_WIDGET_KEY = "project_issue_filter";
  private static final String FILTER_PROPERTY = "filter";
  private static final String DISTRIBUTION_AXIS_PROPERTY = "distributionAxis";

  private final DbClient dbClient;

  public RenameIssueWidgets(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    DbSession session = dbClient.openSession(false);

    try {
      if (dbClient.loadedTemplateDao().countByTypeAndKey(LoadedTemplateDto.ONE_SHOT_TASK_TYPE, getClass().getSimpleName()) != 0) {
        // Already done
        return;
      }

      IssueFilterDto unresolvedIssues = dbClient.issueFilterDao().selectProvidedFilterByName("Unresolved Issues");
      IssueFilterDto hiddenDebt = dbClient.issueFilterDao().selectProvidedFilterByName("False Positive and Won't Fix Issues");
      IssueFilterDto myUnresolvedIssues = dbClient.issueFilterDao().selectProvidedFilterByName("My Unresolved Issues");

      if (unresolvedIssues == null || hiddenDebt == null || myUnresolvedIssues == null) {
        // One of the filter has been deleted, no need to do anything
        return;
      }

      Loggers.get(getClass()).info("Replacing issue related widgets with issue filter widgets");

      List<Long> widgetIdsWithPropertiesToDelete = Lists.newArrayList();
      List<WidgetPropertyDto> widgetPropertiesToCreate = Lists.newArrayList();

      for (WidgetDto widget : dbClient.widgetDao().findAll(session)) {
        switch (widget.getWidgetKey()) {
          case "false_positive_reviews":
            widgetPropertiesToCreate.add(
              new WidgetPropertyDto()
                .setWidgetId(widget.getId())
                .setPropertyKey(FILTER_PROPERTY)
                .setTextValue(hiddenDebt.getId().toString()));
            widgetPropertiesToCreate.add(
              new WidgetPropertyDto()
                .setWidgetId(widget.getId())
                .setPropertyKey(DISTRIBUTION_AXIS_PROPERTY)
                .setTextValue("resolutions"));
            updateWidget(session, widgetIdsWithPropertiesToDelete, widget);
            break;
          case "my_reviews":
            widgetPropertiesToCreate.add(
              new WidgetPropertyDto()
                .setWidgetId(widget.getId())
                .setPropertyKey(FILTER_PROPERTY)
                .setTextValue(myUnresolvedIssues.getId().toString()));
            updateWidget(session, widgetIdsWithPropertiesToDelete, widget);
            break;
          case "reviews_per_developer":
            widgetPropertiesToCreate.add(
              new WidgetPropertyDto()
                .setWidgetId(widget.getId())
                .setPropertyKey(FILTER_PROPERTY)
                .setTextValue(unresolvedIssues.getId().toString()));
            widgetPropertiesToCreate.add(
              new WidgetPropertyDto()
                .setWidgetId(widget.getId())
                .setPropertyKey(DISTRIBUTION_AXIS_PROPERTY)
                .setTextValue("assignees"));
            updateWidget(session, widgetIdsWithPropertiesToDelete, widget);
            break;
          case "unresolved_issues_statuses":
            widgetPropertiesToCreate.add(
              new WidgetPropertyDto()
                .setWidgetId(widget.getId())
                .setPropertyKey(FILTER_PROPERTY)
                .setTextValue(unresolvedIssues.getId().toString()));
            widgetPropertiesToCreate.add(
              new WidgetPropertyDto()
                .setWidgetId(widget.getId())
                .setPropertyKey(DISTRIBUTION_AXIS_PROPERTY)
                .setTextValue("statuses"));
            updateWidget(session, widgetIdsWithPropertiesToDelete, widget);
            break;
          default:
            // Nothing to do, move along
            break;
        }
      }

      dbClient.widgetPropertyDao().deleteByWidgetIds(session, widgetIdsWithPropertiesToDelete);
      dbClient.widgetPropertyDao().insert(session, widgetPropertiesToCreate);

      dbClient.loadedTemplateDao().insert(new LoadedTemplateDto()
        .setType(LoadedTemplateDto.ONE_SHOT_TASK_TYPE)
        .setKey(getClass().getSimpleName()), session);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void updateWidget(DbSession session, List<Long> widgetIdsWithPropertiesToDelete, WidgetDto widget) {
    dbClient.widgetDao().update(session,
      widget.setWidgetKey(PROJECT_ISSUE_FILTER_WIDGET_KEY)
        .setConfigured(true));
    widgetIdsWithPropertiesToDelete.add(widget.getId());
  }

  @Override
  public void stop() {
    // do nothing
  }

}
