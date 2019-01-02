/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.step;

import static java.util.Objects.requireNonNull;

public final class RegisteredMigrationStep {
  private final long migrationNumber;
  private final String description;
  private final Class<? extends MigrationStep> stepClass;

  public RegisteredMigrationStep(long migrationNumber, String description, Class<? extends MigrationStep> migration) {
    this.migrationNumber = migrationNumber;
    this.description = requireNonNull(description, "description can't be null");
    this.stepClass = requireNonNull(migration, "MigrationStep class can't be null");
  }

  public long getMigrationNumber() {
    return migrationNumber;
  }

  public String getDescription() {
    return description;
  }

  public Class<? extends MigrationStep> getStepClass() {
    return stepClass;
  }

  @Override
  public String toString() {
    return "#" + migrationNumber + " '" + description + "'";
  }
}
