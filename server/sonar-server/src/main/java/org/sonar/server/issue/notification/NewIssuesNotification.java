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
import org.sonar.core.component.ComponentDto;
import org.sonar.server.issue.notification.NewIssuesStatistics.METRIC;

import java.util.Date;
import java.util.List;

import static org.sonar.server.issue.notification.NewIssuesEmailTemplate.*;
import static org.sonar.server.issue.notification.NewIssuesStatistics.METRIC.SEVERITY;

public class NewIssuesNotification extends Notification {

  public static final String TYPE = "new-issues";
  private static final String COUNT = ".count";

  public NewIssuesNotification() {
    super(TYPE);
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

  public NewIssuesNotification setStatistics(Component project, NewIssuesStatistics stats) {
    setDefaultMessage(stats.countForMetric(SEVERITY) + " new issues on " + project.longName() + ".\n");

    setSeverityStatistics(stats);
    setTop5CountsForMetric(stats, METRIC.LOGIN);
    setTop5CountsForMetric(stats, METRIC.TAGS);
    setTop5CountsForMetric(stats, METRIC.COMPONENT);

    return this;
  }

  public NewIssuesNotification setDebt(String debt) {
    setFieldValue(METRIC.DEBT + COUNT, debt);
    return this;
  }

  private void setTop5CountsForMetric(NewIssuesStatistics stats, METRIC metric) {
    List<Multiset.Entry<String>> loginStats = stats.statsForMetric(metric);
    for (int i = 0; i < 5 && i < loginStats.size(); i++) {
      setFieldValue(metric + "." + (i + 1) + COUNT, String.valueOf(loginStats.get(i).getCount()));
      setFieldValue(metric + "." + (i + 1) + ".label", loginStats.get(i).getElement());
    }
  }

  private void setSeverityStatistics(NewIssuesStatistics stats) {
    setFieldValue(SEVERITY + COUNT, String.valueOf(stats.countForMetric(SEVERITY)));
    for (String severity : Severity.ALL) {
      setFieldValue(SEVERITY + "." + severity + COUNT, String.valueOf(stats.countForMetric(SEVERITY, severity)));
    }
  }
}
