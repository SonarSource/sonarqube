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
package org.sonar.auth.github;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.auth.github.client.GithubApplicationClient;

import static java.lang.Long.parseLong;
import static java.lang.String.format;

@ComputeEngineSide
public class AppInstallationTokenGenerator {
  private final GithubAppConfiguration githubAppConfiguration;
  private final GithubApplicationClient githubApp;

  AppInstallationTokenGenerator(GithubAppConfiguration githubAppConfiguration, GithubApplicationClient githubApp) {
    this.githubAppConfiguration = githubAppConfiguration;
    this.githubApp = githubApp;
  }

  public ExpiringAppInstallationToken getAppInstallationToken(GithubAppInstallation githubAppInstallation) {
    return githubApp.createAppInstallationToken(githubAppConfiguration, parseLong(githubAppInstallation.installationId()))
      .orElseThrow(() -> new IllegalStateException(format("Error while generating token for GitHub app installed in organization %s", githubAppInstallation.organizationName())));
  }

}
