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

  @Override
  public List<Metric> getMetrics() {
    return List.of(PRIORITIZED_RULE_ISSUES);
  }
}
