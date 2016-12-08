/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.server.platform.db.migration.engine.MigrationContainerPopulatorImpl;
import org.sonar.server.platform.db.migration.engine.MigrationEngineImpl;
import org.sonar.server.platform.db.migration.history.MigrationHistoryImpl;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistryImpl;
import org.sonar.server.platform.db.migration.step.MigrationStepsProvider;

/**
 * Defines the components for the migration engine. This does not include the
 * {@link org.sonar.server.platform.db.migration.version.DbVersion} classes which are bundled together in
 * {@link org.sonar.server.platform.db.migration.version.DbVersionModule}.
 */
public class MigrationEngineModule extends Module {
  @Override
  protected void configureModule() {
    add(
      // migration steps
      MigrationStepRegistryImpl.class,
      new MigrationStepsProvider(),

      // history
      MigrationHistoryImpl.class,

      // engine
      MigrationContainerPopulatorImpl.class,
      MigrationEngineImpl.class);
  }
}
