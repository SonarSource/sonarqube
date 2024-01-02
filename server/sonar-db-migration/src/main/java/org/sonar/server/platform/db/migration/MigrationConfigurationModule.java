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
import org.sonar.server.platform.db.migration.version.v90.DbVersion90;
import org.sonar.server.platform.db.migration.version.v91.DbVersion91;
import org.sonar.server.platform.db.migration.version.v92.DbVersion92;
import org.sonar.server.platform.db.migration.version.v93.DbVersion93;
import org.sonar.server.platform.db.migration.version.v94.DbVersion94;
import org.sonar.server.platform.db.migration.version.v95.DbVersion95;
import org.sonar.server.platform.db.migration.version.v96.DbVersion96;
import org.sonar.server.platform.db.migration.version.v97.DbVersion97;
import org.sonar.server.platform.db.migration.version.v98.DbVersion98;
import org.sonar.server.platform.db.migration.version.v99.DbVersion99;

public class MigrationConfigurationModule extends Module {
  @Override
  protected void configureModule() {
    add(
      MigrationHistoryTableImpl.class,
      // DbVersion implementations
      DbVersion00.class,
      DbVersion90.class,
      DbVersion91.class,
      DbVersion92.class,
      DbVersion93.class,
      DbVersion94.class,
      DbVersion95.class,
      DbVersion96.class,
      DbVersion97.class,
      DbVersion98.class,
      DbVersion99.class,

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
