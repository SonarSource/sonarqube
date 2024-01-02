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
package org.sonar.server.platform.db.migration.version.v97;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion97 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6600, "Add column 'webhook_secret' to 'alm_settings'", AddWebhookSecretToAlmSettingsTable.class)
      .add(6601, "Drop non unique index on 'uuid' in 'components'", DropNonUniqueIndexForComponentsUuid.class)
      .add(6602, "Add unique index on 'uuid' in 'components'", CreateUniqueIndexForComponentsUuid.class)

      .add(6603, "Drop index for 'project_uuid' in 'components'", DropIndexForComponentsProjectUuid.class)
      .add(6604, "Rename column 'project_uuid' to 'branch_uuid' in 'components'", RenameProjectUuidToBranchUuidInComponents.class)
      .add(6605, "Create index for 'branch_uuid' in 'components'", CreateIndexForComponentsBranchUuid.class)

      .add(6606, "Drop index for 'kee' in 'components'", DropIndexForComponentsKey.class)
      .add(6607, "Fix copy component UUID", FixCopyComponentUuid.class)
      .add(6608, "Remove branch information from 'kee' in 'components'", RemoveBranchInformationFromComponentsKey.class)
      .add(6609, "Add unique index on 'kee','branch_uuid' in 'components'", CreateUniqueIndexForComponentsKeeAndBranchUuid.class);
  }
}
