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
package org.sonar.auth.github;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * Token that provides access to the Github API on behalf of
 * the Github organization that installed the Github App.
 *
 * IMPORTANT
 * Two different Github organizations don't share rate limits.
 * Two different instances of {@link ExpiringAppInstallationToken} of the same Github organization
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
public class ExpiringAppInstallationToken implements AppInstallationToken {

  private final Clock clock;
  private final String token;
  private final OffsetDateTime expiresAt;

  public ExpiringAppInstallationToken(Clock clock, String token, String expiresAt) {
    this.clock = clock;
    this.token = requireNonNull(token, "token can't be null");
    this.expiresAt = OffsetDateTime.parse(expiresAt, DateTimeFormatter.ISO_DATE_TIME);
  }

  @Override
  public String getValue() {
    return token;
  }

  @Override
  public String getAuthorizationHeaderPrefix() {
    return "Token";
  }

  public boolean isExpired() {
    return expiresAt.minusMinutes(1).isBefore(OffsetDateTime.now(clock));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExpiringAppInstallationToken that = (ExpiringAppInstallationToken) o;
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
