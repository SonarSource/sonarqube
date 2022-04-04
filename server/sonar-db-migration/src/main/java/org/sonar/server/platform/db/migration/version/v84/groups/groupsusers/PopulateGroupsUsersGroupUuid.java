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
package org.sonar.server.platform.db.migration.version.v84.groups.groupsusers;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateGroupsUsersGroupUuid extends DataChange {

  public PopulateGroupsUsersGroupUuid(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select gu.user_id, gu.group_id " +
      "from groups_users gu " +
      "left outer join groups g on gu.group_id = g.id " +
      "where g.id is null and gu.group_id is not null");

    massUpdate.update("delete from groups_users where user_id = ? and group_id = ?");

    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(1));
      update.setLong(2, row.getLong(2));

      return true;
    });

    massUpdate = context.prepareMassUpdate();

    massUpdate.select("select gu.user_id, gu.group_id, g.uuid " +
      "from groups_users gu " +
      "join groups g on gu.group_id = g.id");

    massUpdate.update("update groups_users set group_uuid = ? where user_id = ? and group_id = ?");

    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(3));
      update.setLong(2, row.getLong(1));
      update.setLong(3, row.getLong(2));

      return true;
    });
  }
}
