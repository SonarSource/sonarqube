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
package org.sonar.server.platform.db.migration;

import org.sonar.core.platform.Module;
import org.sonar.server.platform.db.migration.history.MigrationHistoryImpl;
import org.sonar.server.platform.db.migration.history.MigrationHistoryMeddler;
import org.sonar.server.platform.db.migration.history.MigrationHistoryTableImpl;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistryImpl;
import org.sonar.server.platform.db.migration.step.MigrationStepsProvider;
import org.sonar.server.platform.db.migration.version.v00.DbVersion00;
import org.sonar.server.platform.db.migration.version.v202501.DbVersion202501;
import org.sonar.server.platform.db.migration.version.v202502.DbVersion202502;
import org.sonar.server.platform.db.migration.version.v202503.DbVersion202503;
import org.sonar.server.platform.db.migration.version.v202504.DbVersion202504;
import org.sonar.server.platform.db.migration.version.v202505.DbVersion202505;

public class MigrationConfigurationModule extends Module {
  @Override
  protected void configureModule() {
    add(
      MigrationHistoryTableImpl.class,
      // DbVersion implementations
      DbVersion00.class,
      DbVersion202501.class,
      DbVersion202502.class,
      DbVersion202503.class,
      DbVersion202504.class,
      DbVersion202505.class,

      // migration steps
      MigrationStepRegistryImpl.class,
      new MigrationStepsProvider(),

      // history
      MigrationHistoryImpl.class,
      MigrationHistoryMeddler.class,

      // Utility classes
      DbPrimaryKeyConstraintFinder.class,
      DropPrimaryKeySqlGenerator.class);
  }
}
