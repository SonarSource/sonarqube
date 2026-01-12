/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.dop.jfrog.service;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.server.v2.api.dop.jfrog.response.GateCondition;
import org.sonar.server.v2.api.dop.jfrog.response.GateStatus;
import org.sonar.server.v2.api.dop.jfrog.response.QualityGateEvidence;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubePredicate;

public class JFrogEvidenceMarkdownService {

  private static final Map<String, String> METRIC_NAMES = Map.ofEntries(
    Map.entry("new_reliability_rating", "Reliability Rating on New Code"),
    Map.entry("new_security_rating", "Security Rating on New Code"),
    Map.entry("new_maintainability_rating", "Maintainability Rating on New Code"),
    Map.entry("new_coverage", "Coverage on New Code"),
    Map.entry("new_duplicated_lines_density", "Duplicated Lines on New Code"),
    Map.entry("new_violations", "New Issues"),
    Map.entry("new_blocker_violations", "New Blocker Issues"),
    Map.entry("new_critical_violations", "New Critical Issues"),
    Map.entry("new_major_violations", "New Major Issues"),
    Map.entry("new_minor_violations", "New Minor Issues"),
    Map.entry("new_security_hotspots_reviewed", "Security Hotspots Reviewed on New Code"),
    Map.entry("reliability_rating", "Reliability Rating"),
    Map.entry("security_rating", "Security Rating"),
    Map.entry("sqale_rating", "Maintainability Rating"),
    Map.entry("coverage", "Coverage"),
    Map.entry("duplicated_lines_density", "Duplicated Lines"),
    Map.entry("blocker_violations", "Blocker Issues"),
    Map.entry("critical_violations", "Critical Issues"),
    Map.entry("major_violations", "Major Issues"),
    Map.entry("security_hotspots_reviewed", "Security Hotspots Reviewed"),
    Map.entry("new_software_quality_reliability_rating", "Software Quality Reliability Rating on New Code"),
    Map.entry("new_software_quality_security_rating", "Software Quality Security Rating on New Code"),
    Map.entry("new_software_quality_maintainability_rating", "Software Quality Maintainability Rating on New Code"),
    Map.entry("software_quality_reliability_rating", "Software Quality Reliability Rating"),
    Map.entry("software_quality_security_rating", "Software Quality Security Rating"),
    Map.entry("software_quality_maintainability_rating", "Software Quality Maintainability Rating"));

  private static final Map<GateCondition.Comparator, String> COMPARATOR_DESCRIPTIONS = Map.of(
    GateCondition.Comparator.LT, "is less than",
    GateCondition.Comparator.GT, "is greater than",
    GateCondition.Comparator.EQ, "equals",
    GateCondition.Comparator.NE, "is not equal to");

  public String generateMarkdown(SonarQubePredicate predicate) {
    StringBuilder sb = new StringBuilder();
    sb.append("# SonarQube Quality Gate Evidence\n\n");

    for (QualityGateEvidence gate : predicate.gates()) {
      appendGateSection(sb, gate);
    }

    return sb.toString();
  }

  private static void appendGateSection(StringBuilder sb, QualityGateEvidence gate) {
    sb.append("## Quality Gate Status: ").append(formatStatus(gate.status())).append("\n\n");

    if (gate.ignoredConditions()) {
      sb.append("*Some conditions were ignored because they were not relevant to the analysis.*\n\n");
    }

    List<GateCondition> failedConditions = filterConditionsByStatus(gate.conditions(), GateStatus.ERROR);
    List<GateCondition> passedConditions = filterConditionsByStatus(gate.conditions(), GateStatus.OK);

    appendConditionsSection(sb, failedConditions, "Failed Conditions", "FAILED");
    appendConditionsSection(sb, passedConditions, "Passed Conditions", "PASSED");

    if (failedConditions.isEmpty() && passedConditions.isEmpty() && gate.status() == GateStatus.NONE) {
      sb.append("*No quality gate conditions were evaluated.*\n\n");
    }
  }

  private static List<GateCondition> filterConditionsByStatus(List<GateCondition> conditions, GateStatus status) {
    return conditions.stream()
      .filter(c -> c.status() == status)
      .toList();
  }

  private static void appendConditionsSection(StringBuilder sb, List<GateCondition> conditions, String sectionTitle, String statusLabel) {
    if (conditions.isEmpty()) {
      return;
    }
    sb.append("### ").append(sectionTitle).append("\n\n");
    for (GateCondition condition : conditions) {
      sb.append(formatCondition(condition, statusLabel));
    }
    sb.append("\n");
  }

  private static String formatStatus(GateStatus status) {
    return switch (status) {
      case OK -> "Passed";
      case ERROR -> "Failed";
      case WARN -> "Warning";
      case NONE -> "No Quality Gate";
    };
  }

  private static String formatCondition(GateCondition condition, String statusLabel) {
    String metricName = getMetricName(condition.metricKey());

    StringBuilder sb = new StringBuilder();
    sb.append("- [").append(statusLabel).append("] **").append(metricName).append("**");

    appendConditionDetails(sb, condition);

    sb.append("\n");
    return sb.toString();
  }

  private static void appendConditionDetails(StringBuilder sb, GateCondition condition) {
    String comparator = getComparatorDescription(condition.comparator());
    if (condition.actualValue() != null && condition.errorThreshold() != null && comparator != null) {
      sb.append(": ").append(condition.actualValue())
        .append(" ").append(comparator)
        .append(" ").append(condition.errorThreshold());
    }
  }

  private static String getMetricName(String metricKey) {
    return METRIC_NAMES.getOrDefault(metricKey, metricKey);
  }

  @Nullable
  private static String getComparatorDescription(@Nullable GateCondition.Comparator comparator) {
    if (comparator == null) {
      return null;
    }
    return COMPARATOR_DESCRIPTIONS.get(comparator);
  }
}
