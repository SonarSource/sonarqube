/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateGroupsUsersUserUuid extends DataChange {

  public PopulateGroupsUsersUserUuid(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select gu.group_uuid, gu.user_id, u.uuid " +
      "from groups_users gu " +
      "join users u on gu.user_id = u.id where gu.user_uuid is null");

    massUpdate.update("update groups_users set user_uuid = ? where group_uuid = ? and user_id = ?");

    massUpdate.execute((row, update) -> {
      String groupUuid = row.getString(1);
      long userId = row.getLong(2);
      String userUuid = row.getString(3);
      update.setString(1, userUuid);
      update.setString(2, groupUuid);
      update.setLong(3, userId);
      return true;
    });

    remoteOrphanEntries(context);
  }

  private static void remoteOrphanEntries(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select group_uuid, user_id from groups_users where user_uuid is null");
    massUpdate.update("delete from groups_users where group_uuid = ? and user_id = ?");

    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(1));
      update.setLong(2, row.getLong(2));
      return true;
    });
  }
}
