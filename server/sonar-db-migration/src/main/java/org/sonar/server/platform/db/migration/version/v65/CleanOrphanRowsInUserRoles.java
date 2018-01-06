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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class CleanOrphanRowsInUserRoles extends DataChange {
  public CleanOrphanRowsInUserRoles(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    deleteRowsWithoutComponent(context);
    deleteRowsForNonRootComponent(context);
  }

  private static void deleteRowsWithoutComponent(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select distinct ur.resource_id from user_roles ur where" +
      " ur.resource_id is not null" +
      " and not exists (select id from projects p where p.id = ur.resource_id)");
    massUpdate.rowPluralName("rows without component");
    massUpdate.update("delete from user_roles where resource_id = ?");
    massUpdate.execute(CleanOrphanRowsInUserRoles::handle);
  }

  private static void deleteRowsForNonRootComponent(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select distinct ur.resource_id from user_roles ur" +
      " inner join projects p on p.id = ur.resource_id" +
      " where" +
      " p.scope <> ?" +
      " or (p.qualifier <> ? and p.qualifier <> ?)")
      .setString(1, "PRJ")
      .setString(2, "TRK")
      .setString(3, "VW");
    massUpdate.rowPluralName("rows for non-root component");
    massUpdate.update("delete from user_roles where resource_id = ?");
    massUpdate.execute(CleanOrphanRowsInUserRoles::handle);
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    long resourceId = row.getLong(1);

    update.setLong(1, resourceId);
    return true;
  }
}
