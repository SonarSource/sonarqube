/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.issue;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.rules.RulePriority;

final class SeverityUtils {
  private SeverityUtils() {
    // only static methods
  }

  static Metric severityToIssueMetric(RulePriority severity) {
    Metric metric;
    if (severity.equals(RulePriority.BLOCKER)) {
      metric = CoreMetrics.BLOCKER_ISSUES;
    } else if (severity.equals(RulePriority.CRITICAL)) {
      metric = CoreMetrics.CRITICAL_ISSUES;
    } else if (severity.equals(RulePriority.MAJOR)) {
      metric = CoreMetrics.MAJOR_ISSUES;
    } else if (severity.equals(RulePriority.MINOR)) {
      metric = CoreMetrics.MINOR_ISSUES;
    } else if (severity.equals(RulePriority.INFO)) {
      metric = CoreMetrics.INFO_ISSUES;
    } else {
      throw new IllegalArgumentException("Unsupported severity: " + severity);
    }
    return metric;
  }

  static Metric severityToNewMetricIssue(RulePriority severity) {
    Metric metric;
    if (severity.equals(RulePriority.BLOCKER)) {
      metric = CoreMetrics.NEW_BLOCKER_ISSUES;
    } else if (severity.equals(RulePriority.CRITICAL)) {
      metric = CoreMetrics.NEW_CRITICAL_ISSUES;
    } else if (severity.equals(RulePriority.MAJOR)) {
      metric = CoreMetrics.NEW_MAJOR_ISSUES;
    } else if (severity.equals(RulePriority.MINOR)) {
      metric = CoreMetrics.NEW_MINOR_ISSUES;
    } else if (severity.equals(RulePriority.INFO)) {
      metric = CoreMetrics.NEW_INFO_ISSUES;
    } else {
      throw new IllegalArgumentException("Unsupported severity: " + severity);
    }
    return metric;
  }
}
