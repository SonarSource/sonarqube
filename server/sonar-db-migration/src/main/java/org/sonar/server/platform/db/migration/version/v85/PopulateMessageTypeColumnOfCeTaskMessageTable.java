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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateMessageTypeColumnOfCeTaskMessageTable extends DataChange {
  public PopulateMessageTypeColumnOfCeTaskMessageTable(Database db) {
    super(db);
  }

  @Override
  protected void execute(DataChange.Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select ctm.uuid from ce_task_message ctm where ctm.message_type is null");
    massUpdate.update("update ce_task_message set message_type = ? where uuid = ?");
    massUpdate.execute((row, update) -> {
      update.setString(1, "GENERIC");
      update.setString(2, row.getString(1));
      return true;
    });
  }
}
