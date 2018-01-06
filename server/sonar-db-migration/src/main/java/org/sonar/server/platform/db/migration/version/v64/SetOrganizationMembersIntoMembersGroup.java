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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class SetOrganizationMembersIntoMembersGroup extends DataChange {

  private static final String MEMBERS_NAME = "Members";

  public SetOrganizationMembersIntoMembersGroup(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("users not belonging to 'members' group");
    massUpdate.select("SELECT u.id, g.id FROM users u " +
      "INNER JOIN organization_members om ON om.user_id=u.id " +
      "INNER JOIN groups g ON g.organization_uuid=om.organization_uuid AND g.name=? " +
      "WHERE u.active=? AND NOT EXISTS " +
      "(SELECT 1 FROM groups_users gu " +
      "INNER JOIN groups g on g.id=gu.group_id AND g.name=? " +
      "WHERE gu.user_id=u.id)")
      .setString(1, MEMBERS_NAME)
      .setBoolean(2, true)
      .setString(3, MEMBERS_NAME);
    massUpdate.update("INSERT INTO groups_users (user_id, group_id) values (?, ?)");
    massUpdate.execute((row, update) -> {
      update.setLong(1, row.getLong(1));
      update.setLong(2, row.getLong(2));
      return true;
    });
  }

}
