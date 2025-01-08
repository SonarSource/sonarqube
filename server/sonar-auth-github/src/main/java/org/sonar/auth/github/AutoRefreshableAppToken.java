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

import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
public class AutoRefreshableAppToken implements AppInstallationToken {
  private static final Logger LOG = LoggerFactory.getLogger(AutoRefreshableAppToken.class);

  private final AppInstallationTokenGenerator appInstallationTokenGenerator;
  private final GithubAppInstallation githubAppInstallation;
  private ExpiringAppInstallationToken expiringAppInstallationToken = null;

  public AutoRefreshableAppToken(AppInstallationTokenGenerator appInstallationTokenGenerator, GithubAppInstallation githubAppInstallation) {
    this.appInstallationTokenGenerator = appInstallationTokenGenerator;
    this.githubAppInstallation = githubAppInstallation;
  }

  @Override
  public String getValue() {
    return getAppToken().getValue();
  }

  @Override
  public String getAuthorizationHeaderPrefix() {
    return getAppToken().getAuthorizationHeaderPrefix();
  }

  private ExpiringAppInstallationToken getAppToken() {
    if (expiringAppInstallationToken == null || expiringAppInstallationToken.isExpired()) {
      LOG.debug("Refreshing GitHub app token for organization {}", githubAppInstallation.organizationName());
      expiringAppInstallationToken = appInstallationTokenGenerator.getAppInstallationToken(githubAppInstallation);
    }
    return expiringAppInstallationToken;
  }

}
