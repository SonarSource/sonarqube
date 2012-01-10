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
package org.sonar.plugins.checkstyle;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rules.RulePriority;

public final class CheckstyleSeverityUtils {

  private CheckstyleSeverityUtils() {
    // only static methods
  }

  public static String toSeverity(RulePriority priority) {
    if (RulePriority.BLOCKER.equals(priority) || RulePriority.CRITICAL.equals(priority)) {
      return "error";
    }
    if (RulePriority.MAJOR.equals(priority)) {
      return "warning";
    }
    if (RulePriority.MINOR.equals(priority) || RulePriority.INFO.equals(priority)) {
      return "info";
    }
    throw new IllegalArgumentException("Priority not supported: " + priority);
  }

  public static RulePriority fromSeverity(String severity) {
    if (StringUtils.equals(severity, "error")) {
      return RulePriority.BLOCKER;
    }
    if (StringUtils.equals(severity, "warning")) {
      return RulePriority.MAJOR;
    }
    if (StringUtils.equals(severity, "info")) {
      return RulePriority.INFO;
    }
    return null;
  }
}
