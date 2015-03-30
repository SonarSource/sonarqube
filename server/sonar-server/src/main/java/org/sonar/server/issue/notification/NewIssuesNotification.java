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
package org.sonar.server.issue.notification;

import com.google.common.collect.Multiset;
import org.sonar.api.component.Component;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.notification.NewIssuesStatistics.METRIC;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.sonar.server.issue.notification.NewIssuesEmailTemplate.*;
import static org.sonar.server.issue.notification.NewIssuesStatistics.METRIC.SEVERITY;

public class NewIssuesNotification extends Notification {

  public static final String TYPE = "new-issues";
  private static final long serialVersionUID = -6305871981920103093L;
  private static final String COUNT = ".count";
  private static final String LABEL = ".label";

  private final transient UserIndex userIndex;
  private final transient DbClient dbClient;
  private final transient Durations durations;

  NewIssuesNotification(UserIndex userIndex, DbClient dbClient, Durations durations) {
    this(TYPE, userIndex, dbClient, durations);
  }

  protected NewIssuesNotification(String type, UserIndex userIndex, DbClient dbClient, Durations durations) {
    super(type);
    this.userIndex = userIndex;
    this.dbClient = dbClient;
    this.durations = durations;
  }

  public NewIssuesNotification setAnalysisDate(Date d) {
    setFieldValue(FIELD_PROJECT_DATE, DateUtils.formatDateTime(d));
    return this;
  }

  public NewIssuesNotification setProject(ComponentDto project) {
    setFieldValue(FIELD_PROJECT_NAME, project.longName());
    setFieldValue(FIELD_PROJECT_KEY, project.key());
    setFieldValue(FIELD_PROJECT_UUID, project.uuid());
    return this;
  }

  public NewIssuesNotification setStatistics(Component project, NewIssuesStatistics.Stats stats) {
    setDefaultMessage(stats.countForMetric(SEVERITY) + " new issues on " + project.longName() + ".\n");

    setSeverityStatistics(stats);
    setAssigneesStatistics(stats);
    setTagsStatistics(stats);
    setComponentsStatistics(stats);

    return this;
  }

  protected void setComponentsStatistics(NewIssuesStatistics.Stats stats) {
    METRIC metric = METRIC.COMPONENT;
    List<Multiset.Entry<String>> componentStats = stats.statsForMetric(metric);
    DbSession dbSession = dbClient.openSession(false);
    try {
      for (int i = 0; i < 5 && i < componentStats.size(); i++) {
        String uuid = componentStats.get(i).getElement();
        String componentName = dbClient.componentDao().getByUuid(dbSession, uuid).name();
        setFieldValue(metric + "." + (i + 1) + LABEL, componentName);
        setFieldValue(metric + "." + (i + 1) + COUNT, String.valueOf(componentStats.get(i).getCount()));
      }
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  protected void setTagsStatistics(NewIssuesStatistics.Stats stats) {
    METRIC metric = METRIC.TAGS;
    List<Multiset.Entry<String>> metricStats = stats.statsForMetric(metric);
    for (int i = 0; i < 5 && i < metricStats.size(); i++) {
      setFieldValue(metric + "." + (i + 1) + COUNT, String.valueOf(metricStats.get(i).getCount()));
      setFieldValue(metric + "." + (i + 1) + ".label", metricStats.get(i).getElement());
    }
  }

  protected void setAssigneesStatistics(NewIssuesStatistics.Stats stats) {
    METRIC metric = METRIC.ASSIGNEE;
    List<Multiset.Entry<String>> metricStats = stats.statsForMetric(metric);
    for (int i = 0; i < 5 && i < metricStats.size(); i++) {
      String login = metricStats.get(i).getElement();
      UserDoc user = userIndex.getNullableByLogin(login);
      String name = user == null ? login : user.name();
      setFieldValue(metric + "." + (i + 1) + LABEL, name);
      setFieldValue(metric + "." + (i + 1) + COUNT, String.valueOf(metricStats.get(i).getCount()));
    }
  }

  public NewIssuesNotification setDebt(Duration debt) {
    setFieldValue(METRIC.DEBT + COUNT, durations.format(Locale.ENGLISH, debt));
    return this;
  }

  protected void setSeverityStatistics(NewIssuesStatistics.Stats stats) {
    setFieldValue(SEVERITY + COUNT, String.valueOf(stats.countForMetric(SEVERITY)));
    for (String severity : Severity.ALL) {
      setFieldValue(SEVERITY + "." + severity + COUNT, String.valueOf(stats.countForMetric(SEVERITY, severity)));
    }
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
