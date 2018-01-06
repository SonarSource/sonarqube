/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.health;

import static org.sonar.server.health.Health.newHealthCheckBuilder;

/**
 * Checks the running status of the WebServer when the WebServer is in safemode.
 * Obviously, it statically returns a red health status.
 */
public class WebServerSafemodeNodeCheck implements NodeHealthCheck {

  private static final Health RED_HEALTH = newHealthCheckBuilder()
    .setStatus(Health.Status.RED)
    .addCause("SonarQube webserver is not up")
    .build();

  @Override
  public Health check() {
    return RED_HEALTH;
  }
}
