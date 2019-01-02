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
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class SetKeyTypeToBranchInProjectBranches extends DataChange {
  static final String TABLE_NAME = "project_branches";
  static final String DEFAULT_KEY_TYPE = "BRANCH";

  private final System2 system2;

  public SetKeyTypeToBranchInProjectBranches(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.rowPluralName("branches");
    massUpdate.select("select uuid from " + TABLE_NAME + " where key_type is null");
    massUpdate.update("update " + TABLE_NAME + " set key_type=?, updated_at=? where uuid = ?");
    massUpdate.execute((row, update) -> {
      update.setString(1, DEFAULT_KEY_TYPE);
      update.setLong(2, now);
      update.setString(3, row.getString(1));
      return true;
    });
  }
}
