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
package org.sonar.server.platform.db.migration.version.v75;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion75 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2400, "Add column IS_OWNER_USER in ALM_APP_INSTALLS", AddIsOwnerUserColumnInAlmAppInstall.class)
      .add(2401, "Create ORGANIZATION_ALM_BINDINGS table", CreateOrganizationsAlmBindingsTable.class)
      .add(2402, "Add column USER_EXTERNAL_ID in ALM_APP_INSTALLS", AddUserExternalIdColumnInAlmAppInstall.class)
      .add(2403, "Set IS_OWNER_USER not nullable in ALM_APP_INSTALLS", SetIsOwnerUserNotNullableInAlmAppInstalls.class)
      .add(2404, "Add table EVENT_COMPONENT_CHANGES", AddEventComponentChanges.class);
  }
}
