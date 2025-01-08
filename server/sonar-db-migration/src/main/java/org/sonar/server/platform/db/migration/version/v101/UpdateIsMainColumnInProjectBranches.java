/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class UpdateIsMainColumnInProjectBranches extends DataChange {

  public UpdateIsMainColumnInProjectBranches(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    // we need to use case/when/then because Oracle doesn't accept simple solution uuid = project_uuid here
    massUpdate.select("select uuid, case when uuid = project_uuid then 'true' else 'false' end  from project_branches");
    massUpdate.update("update project_branches set is_main = ? where uuid = ?");
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      boolean isMain = row.getBoolean(2);
      update.setBoolean(1, isMain);
      update.setString(2, uuid);
      return true;
    });

  }
}
