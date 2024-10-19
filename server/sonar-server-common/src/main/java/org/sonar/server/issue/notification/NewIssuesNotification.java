/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Optional;
import java.util.function.ToIntFunction;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.issue.notification.NewIssuesStatistics.Metric;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_BRANCH;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_PROJECT_VERSION;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_PULL_REQUEST;
import static org.sonar.server.issue.notification.NewIssuesEmailTemplate.FIELD_PROJECT_DATE;
import static org.sonar.server.issue.notification.NewIssuesEmailTemplate.FIELD_PROJECT_KEY;
import static org.sonar.server.issue.notification.NewIssuesEmailTemplate.FIELD_PROJECT_NAME;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ISSUE;

public class NewIssuesNotification extends Notification {

  public static final String TYPE = "new-issues";
  private static final long serialVersionUID = -6305871981920103093L;
  private static final String COUNT = ".count";
  private static final String LABEL = ".label";
  private static final String DOT = ".";

  private final transient DetailsSupplier detailsSupplier;

  public NewIssuesNotification(DetailsSupplier detailsSupplier) {
    this(TYPE, detailsSupplier);
  }

  protected NewIssuesNotification(String type, DetailsSupplier detailsSupplier) {
    super(type);
    this.detailsSupplier = detailsSupplier;
  }

  public interface DetailsSupplier {
    /**
     * @throws NullPointerException if {@code ruleKey} is {@code null}
     */
    Optional<RuleDefinition> getRuleDefinitionByRuleKey(RuleKey ruleKey);

    /**
     * @throws NullPointerException if {@code uuid} is {@code null}
     */
    Optional<String> getComponentNameByUuid(String uuid);

    /**
     * @throws NullPointerException if {@code uuid} is {@code null}
     */
    Optional<String> getUserNameByUuid(String uuid);
  }

  public record RuleDefinition(String name, String language) {
    public RuleDefinition(String name, @Nullable String language) {
      this.name = requireNonNull(name, "name can't be null");
      this.language = language;
    }

    @Override
    public String toString() {
      return "RuleDefinition{" + name + " (" + language + ')' + '}';
    }
  }

  public NewIssuesNotification setAnalysisDate(Date d) {
    setFieldValue(FIELD_PROJECT_DATE, DateUtils.formatDateTime(d));
    return this;
  }

  public NewIssuesNotification setProject(String projectKey, String projectName, @Nullable String branchName, @Nullable String pullRequest) {
    setFieldValue(FIELD_PROJECT_NAME, projectName);
    setFieldValue(FIELD_PROJECT_KEY, projectKey);
    if (branchName != null) {
      setFieldValue(FIELD_BRANCH, branchName);
    }
    if (pullRequest != null) {
      setFieldValue(FIELD_PULL_REQUEST, pullRequest);
    }
    return this;
  }

  @CheckForNull
  public String getProjectKey() {
    return getFieldValue(FIELD_PROJECT_KEY);
  }

  public NewIssuesNotification setProjectVersion(@Nullable String version) {
    if (version != null) {
      setFieldValue(FIELD_PROJECT_VERSION, version);
    }
    return this;
  }

  public NewIssuesNotification setStatistics(String projectName, NewIssuesStatistics.Stats stats) {
    setDefaultMessage(stats.getIssueCount().getOnCurrentAnalysis() + " new issues on " + projectName + ".\n");

    setIssueStatistics(stats);
    setAssigneesStatistics(stats);
    setTagsStatistics(stats);
    setComponentsStatistics(stats);
    setRuleStatistics(stats);

    return this;
  }

  private void setRuleStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.RULE;
    List<Map.Entry<String, MetricStatsInt>> fiveBiggest = fiveBiggest(stats.getDistributedMetricStats(metric), MetricStatsInt::getOnCurrentAnalysis);
    int i = 1;
    for (Map.Entry<String, MetricStatsInt> ruleStats : fiveBiggest) {
      String ruleKey = ruleStats.getKey();
      RuleDefinition rule = detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.parse(ruleKey))
        .orElseThrow(() -> new IllegalStateException(String.format("Rule with key '%s' does not exist", ruleKey)));
      String name = rule.name() + " (" + rule.language() + ")";
      setFieldValue(metric + DOT + i + LABEL, name);
      setFieldValue(metric + DOT + i + COUNT, String.valueOf(ruleStats.getValue().getOnCurrentAnalysis()));
      i++;
    }
  }

  private void setComponentsStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.COMPONENT;
    int i = 1;
    List<Map.Entry<String, MetricStatsInt>> fiveBiggest = fiveBiggest(stats.getDistributedMetricStats(metric), MetricStatsInt::getOnCurrentAnalysis);
    for (Map.Entry<String, MetricStatsInt> componentStats : fiveBiggest) {
      String uuid = componentStats.getKey();
      String componentName = detailsSupplier.getComponentNameByUuid(uuid)
        .orElseThrow(() -> new IllegalStateException(String.format("Component with uuid '%s' not found", uuid)));
      setFieldValue(metric + DOT + i + LABEL, componentName);
      setFieldValue(metric + DOT + i + COUNT, String.valueOf(componentStats.getValue().getOnCurrentAnalysis()));
      i++;
    }
  }

  private void setTagsStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.TAG;
    int i = 1;
    for (Map.Entry<String, MetricStatsInt> tagStats : fiveBiggest(stats.getDistributedMetricStats(metric), MetricStatsInt::getOnCurrentAnalysis)) {
      setFieldValue(metric + DOT + i + COUNT, String.valueOf(tagStats.getValue().getOnCurrentAnalysis()));
      setFieldValue(metric + DOT + i + LABEL, tagStats.getKey());
      i++;
    }
  }

  private void setAssigneesStatistics(NewIssuesStatistics.Stats stats) {
    Metric metric = Metric.ASSIGNEE;
    List<Map.Entry<String, MetricStatsInt>> entries = fiveBiggest(stats.getDistributedMetricStats(metric), MetricStatsInt::getOnCurrentAnalysis);

    int i = 1;
    for (Map.Entry<String, MetricStatsInt> assigneeStats : entries) {
      String assigneeUuid = assigneeStats.getKey();
      String name = detailsSupplier.getUserNameByUuid(assigneeUuid).orElse(assigneeUuid);
      setFieldValue(metric + DOT + i + LABEL, name);
      setFieldValue(metric + DOT + i + COUNT, String.valueOf(assigneeStats.getValue().getOnCurrentAnalysis()));
      i++;
    }
  }

  private static List<Map.Entry<String, MetricStatsInt>> fiveBiggest(DistributedMetricStatsInt distributedMetricStatsInt, ToIntFunction<MetricStatsInt> biggerCriteria) {
    Comparator<Map.Entry<String, MetricStatsInt>> comparator = Comparator.comparingInt(a -> biggerCriteria.applyAsInt(a.getValue()));
    return distributedMetricStatsInt.getForLabels()
      .entrySet()
      .stream()
      .filter(i -> biggerCriteria.applyAsInt(i.getValue()) > 0)
      .sorted(comparator.reversed())
      .limit(5)
      .toList();
  }

  private void setIssueStatistics(NewIssuesStatistics.Stats stats) {
    setFieldValue(ISSUE + COUNT, String.valueOf(stats.getIssueCount().getOnCurrentAnalysis()));
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
