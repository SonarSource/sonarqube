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

import javax.annotation.concurrent.Immutable;
import org.sonar.auth.github.security.AccessToken;

import static java.util.Objects.requireNonNull;

/**
 * Token that provides access to the Github API on behalf of
 * the Github organization that installed the Github App.
 *
 * It expires after one hour.
 *
 * IMPORTANT
 * Rate limit is 5'000 API requests per hour for the Github organization.
 * Two different Github organizations don't share rate limits.
 * Two different instances of {@link AppInstallationToken} of the same Github organization
 * share the same quotas (two calls from the two different instances consume
 * two hits).
 *
 * The limit can be higher than 5'000, depending on the number of repositories
 * and users present in the organization. See
 * https://developer.github.com/apps/building-github-apps/understanding-rate-limits-for-github-apps/
 *
 * When the token is expired, the rate limit is 60 calls per hour for the public IP
 * of the machine. BE CAREFUL, THAT SHOULD NEVER OCCUR.
 *
 * See https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#authenticating-as-an-installation
 */
@Immutable
public class AppInstallationToken implements AccessToken {

  private final String token;

  public AppInstallationToken(String token) {
    this.token = requireNonNull(token, "token can't be null");
  }

  @Override
  public String getValue() {
    return token;
  }

  @Override
  public String getAuthorizationHeaderPrefix() {
    return "Token";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppInstallationToken that = (AppInstallationToken) o;
    return token.equals(that.token);
  }

  @Override
  public int hashCode() {
    return token.hashCode();
  }

  @Override
  public String toString() {
    return token;
  }
}
