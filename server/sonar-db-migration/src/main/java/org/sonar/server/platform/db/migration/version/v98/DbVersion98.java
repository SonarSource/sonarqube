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
package org.sonar.server.platform.db.migration.version.v98;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion98 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6700, "Move live measure variations to values", MoveLiveMeasureVariationToValue.class)
      .add(6701, "Drop live measure variation column", DropLiveMeasureVariationColumn.class)
      .add(6702, "Move project measure variations to values", MoveProjectMeasureVariationToValue.class)
      .add(6703, "Drop project measure variation column", DropProjectMeasureVariationColumn.class)
      .add(6704, "Update sonar-users group description", UpsertSonarUsersDescription.class)
      .add(6705, "Add message_formattings column to issue table", AddMessageFormattingsColumnToIssueTable.class)
      .add(6706, "Create scim_users table", CreateScimUsersTable.class)
      .add(6707, "Create unique index on scim_users.user_uuid", CreateUniqueIndexForScimUserUuid.class);
  }
}
