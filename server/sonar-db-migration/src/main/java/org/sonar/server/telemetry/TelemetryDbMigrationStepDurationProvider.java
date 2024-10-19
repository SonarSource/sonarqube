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
package org.sonar.server.telemetry;

import java.util.HashMap;
import java.util.Map;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.core.TelemetryDataType;

public class TelemetryDbMigrationStepDurationProvider implements TelemetryDataProvider<Long> {

  private final Map<String, Long> dbMigrationStepDurations = new HashMap<>();

  @Override
  public String getMetricKey() {
    return "db_migration_step_duration";
  }

  @Override
  public Dimension getDimension() {
    return Dimension.INSTALLATION;
  }

  @Override
  public Granularity getGranularity() {
    return Granularity.ADHOC;
  }

  @Override
  public TelemetryDataType getType() {
    return TelemetryDataType.INTEGER;
  }

  @Override
  public Map<String, Long> getValues() {
    return dbMigrationStepDurations;
  }

  @Override
  public void after() {
    dbMigrationStepDurations.clear();
  }

  public void addCompletedStep(Long migrationId, Long duration) {
    dbMigrationStepDurations.put(migrationId.toString(), duration);
  }
}
