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
package org.sonar.auth.github;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubTeamConverter {

  private static final Pattern TEAM_NAME_PATTERN = Pattern.compile("([^/]+)/(.+)");

  private GithubTeamConverter() {
  }

  public static String toGroupName(GsonTeam team) {
    return toGroupName(team.getOrganizationId(), team.getId());
  }

  public static String toGroupName(String organization, String groupName) {
    return organization + "/" + groupName;
  }

  public static Optional<String> extractOrganizationName(String groupName) {
    return extractRegexGroupIfMatches(groupName, 1);
  }

  public static Optional<String> extractTeamName(String groupName) {
    return extractRegexGroupIfMatches(groupName, 2);
  }

  private static Optional<String> extractRegexGroupIfMatches(String groupName, int regexGroup) {
    Matcher matcher = TEAM_NAME_PATTERN.matcher(groupName);
    if (!matcher.matches()) {
      return Optional.empty();
    } else {
      return Optional.of(matcher.group(regexGroup));
    }
  }
}
