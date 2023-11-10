/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration;

import javax.annotation.Nullable;
import org.sonar.db.DefaultDatabase;
import org.sonar.db.SQDatabase;
import org.sonar.db.TestDb;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class MigrationTestDb implements TestDb {

  private final DefaultDatabase database;

  public MigrationTestDb(@Nullable Class<? extends MigrationStep> migrationStepClass) {
    SQDatabase.Builder builder = new SQDatabase.Builder().asH2Database("sonar");
    if (migrationStepClass != null) {
      builder.createSchema(true).untilMigrationStep(migrationStepClass);
    }
    database = builder.build();
  }

  @Override
  public void start() {
    database.start();
  }

  @Override
  public void stop() {
    database.stop();
  }

  @Override
  public DefaultDatabase getDatabase() {
    return database;
  }
}
