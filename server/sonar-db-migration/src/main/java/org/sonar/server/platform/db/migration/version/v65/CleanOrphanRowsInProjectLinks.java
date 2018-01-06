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

/**
 * Delete rows from table PROJECT_LINKS which either:
 * <ul>
 *   <li>references a non existing component</li>
 *   <li>references a disabled component</li>
 *   <li>references a component which is neither a project nor a view</li>
 * </ul>
 */
public class CleanOrphanRowsInProjectLinks extends DataChange {
  public CleanOrphanRowsInProjectLinks(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      " pl.id" +
      " from project_links pl" +
      " where" +
      "   not exists (" +
      "     select" +
      "       1" +
      "     from projects" +
      "       p" +
      "     where" +
      "       p.uuid = pl.component_uuid" +
      "       and p.scope = ?" +
      "       and p.qualifier in (?,?)" +
      "       and p.enabled = ?" +
      "   )")
      .setString(1, "PRJ")
      .setString(2, "TRK")
      .setString(3, "VW")
      .setBoolean(4, true);
    massUpdate.update("delete from project_links where id = ?");
    massUpdate.rowPluralName("orphan project links");
    massUpdate.execute(CleanOrphanRowsInProjectLinks::handle);
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    long id = row.getLong(1);

    update.setLong(1, id);

    return true;
  }
}
