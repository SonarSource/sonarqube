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
package org.sonar.server.platform.db.migration.version.v72;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion72 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2100, "Increase size of USERS.CRYPTED_PASSWORD", IncreaseCryptedPasswordSize.class)
      .add(2101, "Add HASH_METHOD to table users", AddHashMethodToUsersTable.class)
      .add(2102, "Populate HASH_METHOD on table users", PopulateHashMethodOnUsers.class)
      .add(2103, "Add isExternal boolean to rules", AddRuleExternal.class)
      .add(2104, "Create ALM_APP_INSTALLS table", CreateAlmAppInstallsTable.class)
    ;
  }
}
