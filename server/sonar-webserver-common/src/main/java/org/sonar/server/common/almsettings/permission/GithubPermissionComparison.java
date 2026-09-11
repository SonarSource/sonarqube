/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.common.almsettings.permission;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Compares the permissions a GitHub App or one of its installations actually granted against what the Remediation
 * Agent requires.
 *
 * <p>GitHub access levels are ordered, so a stricter level satisfies a weaker requirement: an installation that
 * granted {@code contents: write} satisfies a {@code contents: read} requirement, but not the other way round. This is
 * deliberately narrower than the app-level check in {@code GithubApplicationClientImpl}, which requires an exact match
 * — matching only the exact level would report a deficit on an installation that granted more than asked for.
 */
final class GithubPermissionComparison {

  private static final Map<String, Integer> ACCESS_LEVELS = Map.of(
    "none", 0,
    "read", 1,
    "write", 2,
    "admin", 3);

  private GithubPermissionComparison() {
    // utility class
  }

  /**
   * The subset of {@code requiredPermissions} that {@code grantedPermissions} does not satisfy, sorted by permission
   * name so that the same deficit always renders the same way.
   */
  static List<PermissionDeficit> findDeficits(Map<String, String> requiredPermissions, Map<String, String> grantedPermissions) {
    return requiredPermissions.entrySet().stream()
      .filter(required -> !satisfies(grantedPermissions.get(required.getKey()), required.getValue()))
      .map(required -> new PermissionDeficit(required.getKey(), required.getValue(), grantedPermissions.get(required.getKey())))
      .sorted((left, right) -> left.permission().compareTo(right.permission()))
      .toList();
  }

  private static boolean satisfies(@Nullable String granted, String required) {
    return levelOf(granted) >= levelOf(required);
  }

  /**
   * An absent permission, and any level GitHub might introduce that we do not know how to rank, count as granting
   * nothing: reporting a deficit an administrator can look at is safer than silently passing a check we cannot make.
   */
  private static int levelOf(@Nullable String level) {
    if (level == null) {
      return 0;
    }
    return ACCESS_LEVELS.getOrDefault(level.toLowerCase(Locale.ENGLISH), 0);
  }
}
