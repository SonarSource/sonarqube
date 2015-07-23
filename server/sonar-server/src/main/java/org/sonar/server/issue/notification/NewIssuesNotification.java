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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.server.issue.notification.NewIssuesStatistics.Metric;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import static org.sonar.server.issue.notification.NewIssuesEmailTemplate.FIELD_PROJECT_DATE;
import static org.sonar.server.issue.notification.NewIssuesEmailTemplate.FIELD_PROJECT_KEY;
import static org.sonar.server.issue.notification.NewIssuesEmailTemplate.FIELD_PROJECT_NAME;
import static org.sonar.server.issue.notification.NewIssuesEmailTemplate.FIELD_PROJECT_UUID;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.SEVERITY;

public class NewIssuesNotification extends Notification {

  public static final String TYPE = "new-issues";
  private static final long serialVersionUID = -6305871981920103093L;
  private static final String COUNT = ".count";
  private static final String LABEL = ".label";
  private static final String DOT = ".";

  private final transient UserIndex userIndex;
  private final transient RuleIndex ruleIndex;
  private final transient DbClient dbClient;
  private final transient Durations durations;

  NewIssuesNotification(UserIndex userIndex, RuleIndex ruleIndex, DbClient dbClient, Durations durations) {
    this(TYPE, userIndex, ruleIndex, dbClient, durations);
  }

  protected NewIssuesNotification(String type, UserIndex userIndex, RuleIndex ruleIndex, DbClient dbClient, Durations durations) {
    super(type);
    this.userIndex = userIndex;
    this.ruleIndex = ruleIndex;
    this.dbClient = dbClient;
    this.durations = durations;
  }

  public NewIssuesNotification setAnalysisDate(Date d) {
    setFieldValue(FIELD_PROJECT_DATE, DateUtils.formatDateTime(d));
    return this;
  }

  public NewIssuesNotification setProject(String projectKey, String projectUuid, String projectName) {
    setFieldValue(FIELD_PROJECT_NAME, projectName);
    setFieldValue(FIELD_PROJECT_KEY, projectKey);
    setFieldValue(FIELD_PROJECT_UUID, projectUuid);
    return this;
  }

  public NewIssuesNotification setStatistics(String projectName, NewIssuesStatistics.Stats stats) {
    setDefaultMessage(stats.countForMetric(SEVERITY) + " new issues on " + projectName + ".\n");

    setSeverityStatistics(stats);
    setAssigneesStatistics(stats);
    setTagsStatistics(stats);
    setComponentsStatistics(stats);
    setRuleStatistics(stats);

    return this;
  }

  protected void setRuleStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.RULE;
    List<Multiset.Entry<String>> metricStats = stats.statsForMetric(metric);
    for (int i = 0; i < 5 && i < metricStats.size(); i++) {
      String ruleKey = metricStats.get(i).getElement();
      Rule rule = ruleIndex.getByKey(RuleKey.parse(ruleKey));
      String name = rule.name() + " (" + rule.language() + ")";
      setFieldValue(metric + DOT + (i + 1) + LABEL, name);
      setFieldValue(metric + DOT + (i + 1) + COUNT, String.valueOf(metricStats.get(i).getCount()));
    }
  }

  protected void setComponentsStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.COMPONENT;
    List<Multiset.Entry<String>> componentStats = stats.statsForMetric(metric);
    DbSession dbSession = dbClient.openSession(false);
    try {
      for (int i = 0; i < 5 && i < componentStats.size(); i++) {
        String uuid = componentStats.get(i).getElement();
        String componentName = dbClient.componentDao().selectOrFailByUuid(dbSession, uuid).name();
        setFieldValue(metric + DOT + (i + 1) + LABEL, componentName);
        setFieldValue(metric + DOT + (i + 1) + COUNT, String.valueOf(componentStats.get(i).getCount()));
      }
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  protected void setTagsStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.TAG;
    List<Multiset.Entry<String>> metricStats = stats.statsForMetric(metric);
    for (int i = 0; i < 5 && i < metricStats.size(); i++) {
      setFieldValue(metric + DOT + (i + 1) + COUNT, String.valueOf(metricStats.get(i).getCount()));
      setFieldValue(metric + DOT + (i + 1) + ".label", metricStats.get(i).getElement());
    }
  }

  protected void setAssigneesStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.ASSIGNEE;
    List<Multiset.Entry<String>> metricStats = stats.statsForMetric(metric);
    for (int i = 0; i < 5 && i < metricStats.size(); i++) {
      String login = metricStats.get(i).getElement();
      UserDoc user = userIndex.getNullableByLogin(login);
      String name = user == null ? login : user.name();
      setFieldValue(metric + DOT + (i + 1) + LABEL, name);
      setFieldValue(metric + DOT + (i + 1) + COUNT, String.valueOf(metricStats.get(i).getCount()));
    }
  }

  public NewIssuesNotification setDebt(Duration debt) {
    setFieldValue(Metric.DEBT + COUNT, durations.format(Locale.ENGLISH, debt));
    return this;
  }

  protected void setSeverityStatistics(NewIssuesStatistics.Stats stats) {
    setFieldValue(SEVERITY + COUNT, String.valueOf(stats.countForMetric(SEVERITY)));
    for (String severity : Severity.ALL) {
      setFieldValue(SEVERITY + DOT + severity + COUNT, String.valueOf(stats.countForMetric(SEVERITY, severity)));
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
