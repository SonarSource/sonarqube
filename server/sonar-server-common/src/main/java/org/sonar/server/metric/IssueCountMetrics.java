/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.metric;

import java.util.List;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import static org.sonar.api.measures.CoreMetrics.DOMAIN_ISSUES;

public class IssueCountMetrics implements Metrics {

  public static final String PRIORITIZED_RULE_ISSUES_KEY = "prioritized_rule_issues";
  public static final Metric<Integer> PRIORITIZED_RULE_ISSUES = new Metric.Builder(
    PRIORITIZED_RULE_ISSUES_KEY, "Issues from prioritized rules", Metric.ValueType.INT)
    .setDescription("Count of issues that have a flag Prioritized Rule.")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String ISSUES_IN_SANDBOX_KEY = "issues_in_sandbox";
  public static final Metric<Integer> ISSUES_IN_SANDBOX = new Metric.Builder(
    ISSUES_IN_SANDBOX_KEY, "Issues in Sandbox", Metric.ValueType.INT)
    .setDescription("Count of issues that are in sandbox status.")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setHidden(true)
    .create();

  public static final String NEW_ISSUES_IN_SANDBOX_KEY = "new_issues_in_sandbox";
  public static final Metric<Integer> NEW_ISSUES_IN_SANDBOX = new Metric.Builder(
    NEW_ISSUES_IN_SANDBOX_KEY, "New Issues in Sandbox", Metric.ValueType.INT)
    .setDescription("Count of new issues that are in sandbox status.")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  @Override
  public List<Metric> getMetrics() {
    return List.of(PRIORITIZED_RULE_ISSUES, ISSUES_IN_SANDBOX, NEW_ISSUES_IN_SANDBOX);
  }
}
