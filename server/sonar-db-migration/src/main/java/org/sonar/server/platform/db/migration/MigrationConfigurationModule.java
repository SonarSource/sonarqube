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
package org.sonar.server.platform.db.migration;

import org.sonar.core.platform.Module;
import org.sonar.server.platform.db.migration.history.MigrationHistoryImpl;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistryImpl;
import org.sonar.server.platform.db.migration.step.MigrationStepsProvider;
import org.sonar.server.platform.db.migration.version.v56.DbVersion56;
import org.sonar.server.platform.db.migration.version.v561.DbVersion561;
import org.sonar.server.platform.db.migration.version.v60.DbVersion60;
import org.sonar.server.platform.db.migration.version.v61.DbVersion61;
import org.sonar.server.platform.db.migration.version.v62.DbVersion62;
import org.sonar.server.platform.db.migration.version.v63.DbVersion63;
import org.sonar.server.platform.db.migration.version.v64.DbVersion64;
import org.sonar.server.platform.db.migration.version.v65.DbVersion65;
import org.sonar.server.platform.db.migration.version.v66.DbVersion66;
import org.sonar.server.platform.db.migration.version.v67.DbVersion67;
import org.sonar.server.platform.db.migration.version.v70.DbVersion70;
import org.sonar.server.platform.db.migration.version.v71.DbVersion71;

public class MigrationConfigurationModule extends Module {
  @Override
  protected void configureModule() {
    add(
      // DbVersion implementations
      DbVersion56.class,
      DbVersion561.class,
      DbVersion60.class,
      DbVersion61.class,
      DbVersion62.class,
      DbVersion63.class,
      DbVersion64.class,
      DbVersion65.class,
      DbVersion66.class,
      DbVersion67.class,
      DbVersion70.class,
      DbVersion71.class,

      // migration steps
      MigrationStepRegistryImpl.class,
      new MigrationStepsProvider(),

      // history
      MigrationHistoryImpl.class);
  }
}
