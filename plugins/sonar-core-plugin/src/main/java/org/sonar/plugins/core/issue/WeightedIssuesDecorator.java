/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.issue;

import com.google.common.base.Strings;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import org.sonar.api.CoreProperties;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.KeyValueFormat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Properties(
  @Property(
    key = CoreProperties.CORE_RULE_WEIGHTS_PROPERTY,
    defaultValue = CoreProperties.CORE_RULE_WEIGHTS_DEFAULT_VALUE,
    name = "Rules weight",
    description = "A weight is associated to each severity to calculate the Rules Compliance Index.",
    project = false,
    global = true,
    category = CoreProperties.CATEGORY_GENERAL)
)
public class WeightedIssuesDecorator implements Decorator {

  private Settings settings;
  private Map<RulePriority, Integer> weightsBySeverity;

  public WeightedIssuesDecorator(Settings settings) {
    this.settings = settings;
  }

  @DependsUpon
  public List<Metric> dependsUponIssues() {
    return Arrays.asList(CoreMetrics.BLOCKER_VIOLATIONS, CoreMetrics.CRITICAL_VIOLATIONS,
      CoreMetrics.MAJOR_VIOLATIONS, CoreMetrics.MINOR_VIOLATIONS, CoreMetrics.INFO_VIOLATIONS);
  }

  @DependedUpon
  public Metric generatesWeightedIssues() {
    return CoreMetrics.WEIGHTED_VIOLATIONS;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void start() {
    weightsBySeverity = getWeights(settings);
  }

  Map<RulePriority, Integer> getWeightsBySeverity() {
    return weightsBySeverity;
  }

  static Map<RulePriority, Integer> getWeights(final Settings settings) {
    String value = settings.getString(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY);

    Map<RulePriority, Integer> weights = KeyValueFormat.parse(value, KeyValueFormat.newPriorityConverter(), KeyValueFormat.newIntegerConverter());

    for (RulePriority priority : RulePriority.values()) {
      if (!weights.containsKey(priority)) {
        weights.put(priority, 1);
      }
    }
    return weights;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    decorate(context);
  }

  void decorate(DecoratorContext context) {
    double debt = 0.0;
    Multiset<RulePriority> distribution = TreeMultiset.create();

    for (RulePriority severity : RulePriority.values()) {
      Measure measure = context.getMeasure(SeverityUtils.severityToIssueMetric(severity));
      if (measure != null && MeasureUtils.hasValue(measure)) {
        distribution.add(severity, measure.getIntValue());
        double add = weightsBySeverity.get(severity) * measure.getIntValue();
        debt += add;
      }
    }

    String distributionFormatted = KeyValueFormat.format(distribution);
    // SONAR-4987 We should store store an empty string for the distribution value
    Measure debtMeasure = new Measure(CoreMetrics.WEIGHTED_VIOLATIONS, debt, Strings.emptyToNull(distributionFormatted));
    context.saveMeasure(debtMeasure);
  }

}
