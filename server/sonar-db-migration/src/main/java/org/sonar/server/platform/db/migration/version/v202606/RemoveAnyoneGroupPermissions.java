/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

/**
 * Permissions granted to the 'Anyone' group are stored as rows with a null 'group_uuid'. The 'Anyone' group is being
 * removed: {@link MigrateAnyoneGroupPermissionsToSonarUsers} first migrates every such row to 'sonar-users', then
 * this step deletes every remaining null-group row.
 */
public class RemoveAnyoneGroupPermissions extends DataChange {

  private static final String DELETE_GROUP_ROLES_SQL = "DELETE FROM group_roles WHERE group_uuid IS NULL";
  private static final String DELETE_PERM_TEMPLATES_GROUPS_SQL = "DELETE FROM perm_templates_groups WHERE group_uuid IS NULL";

  public RemoveAnyoneGroupPermissions(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    context.prepareUpsert(DELETE_GROUP_ROLES_SQL).execute().commit();
    context.prepareUpsert(DELETE_PERM_TEMPLATES_GROUPS_SQL).execute().commit();
  }
}
