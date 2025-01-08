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
package org.sonar.alm.client.gitlab;

import java.util.Optional;
import org.sonar.alm.client.DevopsPlatformHeaders;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

@ServerSide
@ComputeEngineSide
public class GitlabHeaders implements DevopsPlatformHeaders {

  @Override
  public Optional<String> getApiVersionHeader() {
    return Optional.empty();
  }

  @Override
  public Optional<String> getApiVersion() {
    return Optional.empty();
  }

  @Override
  public String getRateLimitRemainingHeader() {
    return "ratelimit-remaining";
  }

  @Override
  public String getRateLimitLimitHeader() {
    return "ratelimit-limit";
  }

  @Override
  public String getRateLimitResetHeader() {
    return "ratelimit-reset";
  }

  @Override
  public String getAuthorizationHeader() {
    return "PRIVATE-TOKEN";
  }
}
