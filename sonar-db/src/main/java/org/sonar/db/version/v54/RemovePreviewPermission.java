/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v54;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

/**
 * Remove the "Execute Preview Analysis" (dryRunScan) permission
 */
public class RemovePreviewPermission extends BaseDataChange {

  public RemovePreviewPermission(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    execute(context, "group_roles", "groups");
    execute(context, "user_roles", "users");
  }

  private static void execute(Context context, String tableName, String displayName) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName(displayName);
    update.select("SELECT r.id FROM " + tableName + " r WHERE r.role=?").setString(1, "dryRunScan");
    update.update("DELETE FROM " + tableName + " WHERE id=?");
    update.execute(MigrationHandler.INSTANCE);
  }

  private enum MigrationHandler implements MassUpdate.Handler {
    INSTANCE;

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      update.setLong(1, row.getLong(1));
      return true;
    }
  }
}
