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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public abstract class PopulateMainComponentUuidColumnsToCeTable extends DataChange {
  protected final String tableName;

  PopulateMainComponentUuidColumnsToCeTable(Database db, String tableName) {
    super(db);
    this.tableName = tableName;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      "  c.uuid, c.tmp_component_uuid, c.tmp_main_component_uuid" +
      " from " + tableName + " c" +
      " where" +
      "  c.tmp_component_uuid is not null" +
      "  and (c.component_uuid is null or c.main_component_uuid is null)");
    massUpdate.rowPluralName("tasks with component");
    massUpdate.update("update " + tableName + " set component_uuid=?, main_component_uuid=? where uuid=?");
    massUpdate.execute(PopulateMainComponentUuidColumnsToCeTable::handleUpdate);
  }

  private static boolean handleUpdate(Select.Row row, SqlStatement update) throws SQLException {
    String uuid = row.getString(1);
    String componentUuuid = row.getString(2);
    String mainComponentUuuid = row.getString(3);

    update.setString(1, componentUuuid);
    update.setString(2, mainComponentUuuid);
    update.setString(3, uuid);

    return true;
  }
}
