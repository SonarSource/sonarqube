/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client.github.security;

import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * JWT (Json Web Token) to authenticate API requests on behalf
 * of the SonarCloud App.
 *
 * Token expires after {@link #EXPIRATION_PERIOD_IN_MINUTES} minutes.
 *
 * IMPORTANT
 * Rate limit is 5'000 API requests per hour for ALL the clients
 * of the SonarCloud App (all instances of {@link AppToken} from Compute Engines/web servers
 * and from the other SonarSource services using the App). For example three calls with
 * three different tokens will consume 3 hits. Remaining quota will be 4'997.
 * When the token is expired, the rate limit is 60 calls per hour for the public IP
 * of the machine. BE CAREFUL, THAT SHOULD NEVER OCCUR.
 *
 * See https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#authenticating-as-a-github-app
 */
@Immutable
public class AppToken implements AccessToken {

  // SONARCLOUD-468 maximum allowed by GitHub is 10 minutes but we use 9 minutes just in case clocks are not synchronized
  static final int EXPIRATION_PERIOD_IN_MINUTES = 9;

  private final String jwt;

  public AppToken(String jwt) {
    this.jwt = requireNonNull(jwt, "jwt can't be null");
  }

  @Override
  public String getValue() {
    return jwt;
  }

  @Override
  public String getAuthorizationHeaderPrefix() {
    return "Bearer";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppToken appToken = (AppToken) o;
    return jwt.equals(appToken.jwt);
  }

  @Override
  public int hashCode() {
    return jwt.hashCode();
  }

  @Override
  public String toString() {
    return jwt;
  }
}
