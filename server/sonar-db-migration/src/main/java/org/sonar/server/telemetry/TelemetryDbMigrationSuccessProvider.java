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

import java.util.Optional;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

public class TelemetryDbMigrationSuccessProvider extends AbstractTelemetryDataProvider<Boolean> {

  private Boolean dbMigrationSuccess = null;

  public TelemetryDbMigrationSuccessProvider() {
    super("db_migration_success", Dimension.INSTALLATION, Granularity.ADHOC, TelemetryDataType.BOOLEAN);
  }

  public void setDbMigrationSuccess(Boolean dbMigrationSuccess) {
    this.dbMigrationSuccess = dbMigrationSuccess;
  }

  @Override
  public Optional<Boolean> getValue() {
    return Optional.ofNullable(dbMigrationSuccess);
  }

  @Override
  public void after() {
    dbMigrationSuccess = null;
  }
}
