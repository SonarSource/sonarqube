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
package org.sonar.server.platform.telemetry;

import org.sonar.api.platform.Server;
import org.sonar.telemetry.Dimension;
import org.sonar.telemetry.Granularity;
import org.sonar.telemetry.TelemetryDataProvider;
import org.sonar.telemetry.TelemetryDataType;

public class TelemetryVersionProvider implements TelemetryDataProvider<String> {

  private final Server server;

  public TelemetryVersionProvider(Server server) {
    this.server = server;
  }

  @Override
  public String getMetricKey() {
    return "version";
  }

  @Override
  public Dimension getDimension() {
    return Dimension.INSTALLATION;
  }

  @Override
  public Granularity getGranularity() {
    return Granularity.DAILY;
  }

  @Override
  public TelemetryDataType getType() {
    return TelemetryDataType.STRING;
  }

  @Override
  public String getValue() {
    return server.getVersion();
  }
}
