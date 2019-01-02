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

import java.util.Arrays;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.server.platform.db.migration.version.DbVersion;

/**
 * This class is responsible for providing the {@link MigrationSteps} to be injected in classes that need it and
 * ensures that there's only one such instance.
 */
public class MigrationStepsProvider extends ProviderAdapter {
  private MigrationSteps migrationSteps;

  public MigrationSteps provide(InternalMigrationStepRegistry migrationStepRegistry, DbVersion... dbVersions) {
    if (migrationSteps == null) {
      migrationSteps = buildMigrationSteps(migrationStepRegistry, dbVersions);
    }
    return migrationSteps;
  }

  private static MigrationSteps buildMigrationSteps(InternalMigrationStepRegistry migrationStepRegistry, DbVersion[] dbVersions) {
    Arrays.stream(dbVersions).forEach(dbVersion -> dbVersion.addSteps(migrationStepRegistry));
    return migrationStepRegistry.build();
  }
}
