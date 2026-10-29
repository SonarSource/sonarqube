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
package org.sonar.server.common.almsettings.github;

public final class GithubErrorMessages {

  public static final String MISSING_USER_AUTHORIZATION = "No GitHub authorization found for your user. "
    + "Authorize SonarQube on GitHub from Projects > Create Project > From GitHub, then try again.";

  public static final String REPOSITORY_NOT_FOUND_TEMPLATE = "Impossible to find the repository '%s' on GitHub, using the devops config %s. "
    + "Check that the GitHub App is installed on the organization that owns the repository, and that your GitHub account can access it.";

  private GithubErrorMessages() {
  }
}
