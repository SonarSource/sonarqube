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
package org.sonar.server.platform.db.migration.version.v83.grouproles;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class MigrateResourceIdToUuidInGroupRoles extends DataChange {
  public MigrateResourceIdToUuidInGroupRoles(Database db) {
    super(db);
  }

  @Override protected void execute(Context context) throws SQLException {
    // remove roles associated with invalid resource
    context.prepareUpsert("delete from group_roles "
      + "where group_roles.resource_id is not null and not exists (select 1 from components c where group_roles.resource_id = c.id)")
      .execute();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select gp.id as gp_id, c.uuid as c_uuid from group_roles gp, components c where gp.resource_id = c.id and gp.component_uuid is null");
    massUpdate.update("update group_roles set component_uuid = ? where id = ?");

    massUpdate.execute((row, update) -> {
      String componentUuid = row.getString(2);
      Long propertyId = row.getLong(1);

      update.setString(1, componentUuid)
        .setLong(2, propertyId);
      return true;
    });
  }
}
