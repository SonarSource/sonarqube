/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class CleanCeChildTablesOrphans extends DataChange {
  public CleanCeChildTablesOrphans(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    deleteOrphansInCeChildTable(context, "ce_task_input");
    deleteOrphansInCeChildTable(context, "ce_scanner_context");
    deleteOrphansInCeChildTable(context, "ce_task_characteristics");
  }

  private static void deleteOrphansInCeChildTable(Context context, String childTableName) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select task_uuid from " + childTableName + " child where" +
      " not exists (select 1 from ce_activity a where a.uuid = child.task_uuid)" +
      " and not exists (select 1 from ce_queue q where q.uuid = child.task_uuid)");
    massUpdate.rowPluralName("orphans rows in " + childTableName);
    massUpdate.update("delete from " + childTableName + " where task_uuid=?");
    massUpdate.execute(CleanCeChildTablesOrphans::deleteByTaskUuid);
  }

  private static boolean deleteByTaskUuid(Select.Row row, SqlStatement update) throws SQLException {
    String taskUuid = row.getString(1);
    update.setString(1, taskUuid);
    return true;
  }
}
