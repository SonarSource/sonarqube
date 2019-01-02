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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class SupportProjectVisibilityInTemplates extends DataChange {
  public SupportProjectVisibilityInTemplates(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select distinct template_id from perm_templates_groups" +
      " where" +
      " group_id is null" +
      " and (permission_reference='user' or permission_reference='codeviewer')");
    massUpdate.update("delete from perm_templates_groups where" +
      " template_id = ?" +
      " and group_id is null" +
      " and (permission_reference='user' or permission_reference='codeviewer')");
    massUpdate.rowPluralName("permission templates with useless permissions to group AnyOne");
    massUpdate.execute(SupportProjectVisibilityInTemplates::handle);
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    int templateId = row.getInt(1);

    update.setInt(1, templateId);
    return true;
  }
}
