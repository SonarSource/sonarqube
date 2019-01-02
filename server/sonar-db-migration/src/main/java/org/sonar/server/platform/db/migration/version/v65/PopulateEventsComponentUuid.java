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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class PopulateEventsComponentUuid extends DataChange {
  public PopulateEventsComponentUuid(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    populateColumnOrFixInconsistencies(context);

    deleteOrphans(context);
  }

  private static void populateColumnOrFixInconsistencies(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      "   e.id, s.component_uuid" +
      " from events e" +
      " inner join snapshots s on" +
      "   s.uuid = e.analysis_uuid" +
      " where" +
      "   e.component_uuid is null" +
      "   or e.component_uuid <> s.uuid");
    massUpdate.update("update events set component_uuid = ? where id = ?");
    massUpdate.rowPluralName("events without component_uuid");
    massUpdate.execute(PopulateEventsComponentUuid::handlePopulate);
  }

  private static boolean handlePopulate(Select.Row row, SqlStatement update) throws SQLException {
    long id = row.getLong(1);
    String projectUuid = row.getString(2);

    update.setString(1, projectUuid);
    update.setLong(2, id);

    return true;
  }

  private static void deleteOrphans(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id from events e where" +
      " e.component_uuid is null" +
      " or not exists (select id from snapshots s where s.uuid = e.analysis_uuid)");
    massUpdate.update("delete from events where id = ?");
    massUpdate.rowPluralName("delete orphan events");
    massUpdate.execute(PopulateEventsComponentUuid::handleDelete);
  }

  private static boolean handleDelete(Select.Row row, SqlStatement update) throws SQLException {
    long id = row.getLong(1);

    update.setLong(1, id);

    return true;
  }
}
