/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.issue.notification;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.issue.notification.NewIssuesStatistics.Metric;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_BRANCH;
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

  public NewIssuesNotification setProject(String projectKey, String projectUuid, String projectName, @Nullable String branchName) {
    setFieldValue(FIELD_PROJECT_NAME, projectName);
    setFieldValue(FIELD_PROJECT_KEY, projectKey);
    setFieldValue(FIELD_PROJECT_UUID, projectUuid);
    if (branchName != null) {
      setFieldValue(FIELD_BRANCH, branchName);
    }
    return this;
  }

  public NewIssuesNotification setStatistics(String projectName, NewIssuesStatistics.Stats stats) {
    setDefaultMessage(stats.getDistributedMetricStats(SEVERITY).getTotal() + " new issues on " + projectName + ".\n");

    try (DbSession dbSession = dbClient.openSession(false)) {
      setSeverityStatistics(stats);
      setAssigneesStatistics(stats);
      setTagsStatistics(stats);
      setComponentsStatistics(dbSession, stats);
      setRuleStatistics(dbSession, stats);
    }

    return this;
  }

  private void setRuleStatistics(DbSession dbSession, NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.RULE;
    int i = 1;
    for (Map.Entry<String, MetricStatsInt> ruleStats : fiveBiggest(stats.getDistributedMetricStats(metric), MetricStatsInt::getTotal)) {
      String ruleKey = ruleStats.getKey();
      RuleDefinitionDto rule = dbClient.ruleDao().selectOrFailDefinitionByKey(dbSession, RuleKey.parse(ruleKey));
      String name = rule.getName() + " (" + rule.getLanguage() + ")";
      setFieldValue(metric + DOT + i + LABEL, name);
      setFieldValue(metric + DOT + i + COUNT, String.valueOf(ruleStats.getValue().getTotal()));
      i++;
    }
  }

  private void setComponentsStatistics(DbSession dbSession, NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.COMPONENT;
    int i = 1;
    for (Map.Entry<String, MetricStatsInt> componentStats : fiveBiggest(stats.getDistributedMetricStats(metric), MetricStatsInt::getTotal)) {
      String uuid = componentStats.getKey();
      String componentName = dbClient.componentDao().selectOrFailByUuid(dbSession, uuid).name();
      setFieldValue(metric + DOT + i + LABEL, componentName);
      setFieldValue(metric + DOT + i + COUNT, String.valueOf(componentStats.getValue().getTotal()));
      i++;
    }
  }

  private void setTagsStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.TAG;
    int i = 1;
    for (Map.Entry<String, MetricStatsInt> tagStats : fiveBiggest(stats.getDistributedMetricStats(metric), MetricStatsInt::getTotal)) {
      setFieldValue(metric + DOT + i + COUNT, String.valueOf(tagStats.getValue().getTotal()));
      setFieldValue(metric + DOT + i + ".label", tagStats.getKey());
      i++;
    }
  }

  private void setAssigneesStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.ASSIGNEE;
    ToIntFunction<MetricStatsInt> biggerCriteria = MetricStatsInt::getTotal;
    int i = 1;
    for (Map.Entry<String, MetricStatsInt> assigneeStats : fiveBiggest(stats.getDistributedMetricStats(metric), biggerCriteria)) {
      String login = assigneeStats.getKey();
      UserDoc user = userIndex.getNullableByLogin(login);
      String name = user == null ? login : user.name();
      setFieldValue(metric + DOT + i + LABEL, name);
      setFieldValue(metric + DOT + i + COUNT, String.valueOf(biggerCriteria.applyAsInt(assigneeStats.getValue())));
      i++;
    }
  }

  private static List<Map.Entry<String, MetricStatsInt>> fiveBiggest(DistributedMetricStatsInt distributedMetricStatsInt, ToIntFunction<MetricStatsInt> biggerCriteria) {
    Comparator<Map.Entry<String, MetricStatsInt>> comparator = Comparator.comparingInt(a -> biggerCriteria.applyAsInt(a.getValue()));
    return distributedMetricStatsInt.getForLabels()
      .entrySet()
      .stream()
      .sorted(comparator.reversed())
      .limit(5)
      .collect(MoreCollectors.toList(5));
  }

  public NewIssuesNotification setDebt(Duration debt) {
    setFieldValue(Metric.EFFORT + COUNT, durations.format(debt));
    return this;
  }

  private void setSeverityStatistics(NewIssuesStatistics.Stats stats) {
    DistributedMetricStatsInt distributedMetricStats = stats.getDistributedMetricStats(SEVERITY);
    setFieldValue(SEVERITY + COUNT, String.valueOf(distributedMetricStats.getTotal()));
    for (String severity : Severity.ALL) {
      setFieldValue(
        SEVERITY + DOT + severity + COUNT,
        String.valueOf(distributedMetricStats.getForLabel(severity).map(MetricStatsInt::getTotal).orElse(0)));
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
