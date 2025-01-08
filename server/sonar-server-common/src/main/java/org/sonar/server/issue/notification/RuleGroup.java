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
package org.sonar.server.issue.notification;

import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.api.rules.RuleType;

import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;

enum RuleGroup {
  SECURITY_HOTSPOTS,
  ISSUES;

  static RuleGroup resolveGroup(@Nullable RuleType ruleType) {
    return SECURITY_HOTSPOT.equals(ruleType) ? SECURITY_HOTSPOTS : ISSUES;
  }

  static String formatIssuesOrHotspots(Collection<?> issues, Collection<?> hotspots) {
    if (!issues.isEmpty() && !hotspots.isEmpty()) {
      return "issues/hotspots";
    }
    if (issues.size() == 1) {
      return "an issue";
    }
    if (issues.size() > 1) {
      return "issues";
    }
    if (hotspots.size() == 1) {
      return "a hotspot";
    }
    return "hotspots";
  }

  static String formatIssueOrHotspot(@Nullable RuleType ruleType) {
    if (SECURITY_HOTSPOT.equals(ruleType)) {
      return "hotspot";
    }
    return "issue";
  }

  static String formatIssuesOrHotspots(@Nullable RuleType ruleType) {
    if (SECURITY_HOTSPOT.equals(ruleType)) {
      return "hotspots";
    }
    return "issues";
  }
}
