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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

@SupportsBlueGreen
public class PopulateTmpLastKeyColumnsToCeActivity extends DataChange {

  public PopulateTmpLastKeyColumnsToCeActivity(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      "  cea.uuid, cea.task_type, cea.component_uuid, cea.tmp_component_uuid, cea.tmp_main_component_uuid" +
      " from ce_activity cea" +
      " where" +
      "  cea.tmp_is_last is null" +
      "  or cea.tmp_is_last_key is null" +
      "  or cea.tmp_main_is_last is null" +
      "  or cea.tmp_main_is_last_key is null");
    massUpdate.update("update ce_activity" +
      " set" +
      "  tmp_is_last=?" +
      "  ,tmp_is_last_key=?" +
      "  ,tmp_main_is_last=?" +
      "  ,tmp_main_is_last_key=?" +
      " where uuid=?");
    massUpdate.rowPluralName("rows of ce_activity");
    massUpdate.execute(PopulateTmpLastKeyColumnsToCeActivity::handleUpdate);
  }

  private static boolean handleUpdate(Select.Row row, SqlStatement update) throws SQLException {
    String uuid = row.getString(1);
    String taskType = row.getString(2);
    String oldComponentUuid = row.getString(3);
    String componentUuuid = row.getString(4);
    String mainComponentUuuid = row.getString(5);

    update.setBoolean(1, false);
    update.setString(2, oldComponentUuid == null ? taskType : (taskType + componentUuuid));
    update.setBoolean(3, false);
    update.setString(4, oldComponentUuid == null ? taskType : (taskType + mainComponentUuuid));
    update.setString(5, uuid);

    return true;
  }
}
