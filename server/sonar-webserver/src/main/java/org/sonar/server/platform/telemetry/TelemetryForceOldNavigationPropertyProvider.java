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
package org.sonar.server.platform.telemetry;

import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.core.config.WebConstants.SONAR_UI_FORCE_OLD_NAVIGATION;
import static org.sonar.telemetry.core.Dimension.INSTALLATION;
import static org.sonar.telemetry.core.Granularity.WEEKLY;
import static org.sonar.telemetry.core.TelemetryDataType.BOOLEAN;

public class TelemetryForceOldNavigationPropertyProvider implements TelemetryDataProvider<Boolean> {
  private final Configuration config;

  public TelemetryForceOldNavigationPropertyProvider(Configuration config) {
    this.config = config;
  }

  @Override
  public String getMetricKey() {
    return "force_old_navigation_enabled";
  }

  @Override
  public Dimension getDimension() {
    return INSTALLATION;
  }

  @Override
  public Granularity getGranularity() {
    return WEEKLY;
  }

  @Override
  public TelemetryDataType getType() {
    return BOOLEAN;
  }

  @Override
  public Optional<Boolean> getValue() {
    return Optional.of(config.getBoolean(SONAR_UI_FORCE_OLD_NAVIGATION).orElse(false));
  }
}