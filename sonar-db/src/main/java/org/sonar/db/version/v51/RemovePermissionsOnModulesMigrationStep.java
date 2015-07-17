/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.db.version.v51;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

/**
 * See http://jira.sonarsource.com/browse/SONAR-5596
 *
 * It's no possible to set permission on a module or a sub-view, but the batch was setting default permission on it on their creation.
 * As now it's no more the case, we need to purge this useless data.
 *
 * @since 5.1
 */
public class RemovePermissionsOnModulesMigrationStep extends BaseDataChange {

  public RemovePermissionsOnModulesMigrationStep(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    removeUserRolePermissions(context, "user_roles", "user roles");
    removeUserRolePermissions(context, "group_roles", "group roles");
  }

  private void removeUserRolePermissions(Context context, String tableName, String pluralName) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT r.id " +
      "FROM " + tableName + " r " +
      "  INNER JOIN projects ON projects.id = r.resource_id " +
      "WHERE projects.module_uuid IS NOT NULL");
    massUpdate.update("DELETE FROM " + tableName + " WHERE id=?");
    massUpdate.rowPluralName(pluralName);
    massUpdate.execute(MigrationHandler.INSTANCE);
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
