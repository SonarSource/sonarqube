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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import java.util.UUID;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class AddArchitectureAdminPermissionToAdminsGroup extends DataChange {

  private static final String ADMINS_GROUP = "sonar-administrators";
  private static final String ADMIN_PERMISSION = "admin";
  private static final String ARCHITECTURE_ADMIN_PERMISSION = "architectureadmin";

  public AddArchitectureAdminPermissionToAdminsGroup(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String groupUuid = context.prepareSelect("select uuid from groups where name = ?")
      .setString(1, ADMINS_GROUP)
      .get(row -> row.getString(1));
    if (groupUuid == null) {
      return;
    }

    if (!hasGlobalRole(context, groupUuid, ADMIN_PERMISSION)) {
      return;
    }

    if (hasGlobalRole(context, groupUuid, ARCHITECTURE_ADMIN_PERMISSION)) {
      return;
    }

    context.prepareUpsert("insert into group_roles (uuid, group_uuid, entity_uuid, role) values (?, ?, null, ?)")
      .setString(1, UUID.randomUUID().toString())
      .setString(2, groupUuid)
      .setString(3, ARCHITECTURE_ADMIN_PERMISSION)
      .execute()
      .commit();
  }

  private static boolean hasGlobalRole(Context context, String groupUuid, String role) throws SQLException {
    Long count = context.prepareSelect("select count(*) from group_roles where group_uuid = ? and entity_uuid is null and role = ?")
      .setString(1, groupUuid)
      .setString(2, role)
      .get(row -> row.getLong(1));
    return count != null && count > 0;
  }
}
