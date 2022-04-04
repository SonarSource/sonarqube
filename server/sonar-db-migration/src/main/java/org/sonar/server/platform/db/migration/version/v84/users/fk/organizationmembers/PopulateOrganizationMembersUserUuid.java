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
package org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateOrganizationMembersUserUuid extends DataChange {

  public PopulateOrganizationMembersUserUuid(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select om.user_id, om.organization_uuid, u.uuid " +
      "from organization_members om " +
      "join users u on om.user_id = u.id where om.user_uuid is null");

    massUpdate.update("update organization_members set user_uuid = ? where organization_uuid = ? and user_id = ?");

    massUpdate.execute((row, update, index) -> {
      long userId = row.getLong(1);
      String organizationUuid = row.getString(2);
      String userUuid = row.getString(3);

      update.setString(1, userUuid);
      update.setString(2, organizationUuid);
      update.setLong(3, userId);
      return true;
    });

    MassUpdate.Handler removeOrphanHandler = (row, update) -> {
      update.setString(1, row.getString(1));
      update.setLong(2, row.getLong(2));

      return true;
    };
    massUpdate = context.prepareMassUpdate();

    massUpdate.select("select organization_uuid, user_id from organization_members where user_uuid is null");
    massUpdate.update("delete from organization_members where organization_uuid = ? and user_id = ?");

    massUpdate.execute(removeOrphanHandler);
  }
}
