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
package org.sonar.db;

import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class MigrationDbTester extends AbstractSqlDbTester<MigrationTestDb> implements BeforeEachCallback, AfterEachCallback {

  private MigrationDbTester(@Nullable Class<? extends MigrationStep> migrationStepClass) {
    super(new MigrationTestDb(migrationStepClass));
  }

  public static MigrationDbTester createEmpty() {
    return new MigrationDbTester(null);
  }

  public static MigrationDbTester createForMigrationStep(Class<? extends MigrationStep> migrationStepClass) {
    return new MigrationDbTester(migrationStepClass);
  }
}
