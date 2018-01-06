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

public class PurgeDeveloperData extends DataChange {
  public PurgeDeveloperData(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    purgeProjectCopies(context);

    purgeDevelopers(context);
  }

  private void purgeProjectCopies(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      "   child.id, child.uuid" +
      " from projects child" +
      " inner join projects root on" +
      "   root.uuid = child.project_uuid" +
      "   and root.scope=?" +
      "   and root.qualifier=?" +
      " where" +
      "   child.uuid <> child.project_uuid")
      .setString(1, "PRJ")
      .setString(2, "DEV");
    massUpdate.rowPluralName("purged project copies");
    massUpdate.update("delete from project_measures where component_uuid=?");
    massUpdate.update("delete from projects where uuid=?");
    massUpdate.execute(PurgeDeveloperData::handlePurgeProjectCopies);
  }

  private static boolean handlePurgeProjectCopies(Select.Row row, SqlStatement update, int updateIndex) throws SQLException {
    if (updateIndex < 0 || updateIndex > 1) {
      throw new IllegalArgumentException("Unsupported updateIndex " + updateIndex);
    }
    String uuid = row.getString(2);
    update.setString(1, uuid);
    return true;
  }

  private void purgeDevelopers(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      "   id, uuid" +
      " from projects" +
      " where" +
      "   scope=?" +
      "   and qualifier=?")
      .setString(1, "PRJ")
      .setString(2, "DEV");
    massUpdate.update("delete from project_measures where component_uuid=?");
    massUpdate.update("delete from ce_activity where component_uuid=?");
    massUpdate.update("delete from snapshots where component_uuid=?");
    massUpdate.update("delete from group_roles where resource_id=?");
    massUpdate.update("delete from user_roles where resource_id=?");
    massUpdate.update("delete from projects where project_uuid=?");
    massUpdate.rowPluralName("purged developers");
    massUpdate.execute(PurgeDeveloperData::handlePurgeDevelopers);
  }

  private static boolean handlePurgeDevelopers(Select.Row row, SqlStatement update, int updateIndex) throws SQLException {
    long id = row.getLong(1);
    String uuid = row.getString(2);
    switch (updateIndex) {
      case 0:
      case 1:
      case 2:
        update.setString(1, uuid);
        return true;
      case 3:
      case 4:
        update.setLong(1, id);
        return true;
      case 5:
        update.setString(1, uuid);
        return true;
      default:
        throw new IllegalArgumentException("Unsupported updateIndex " + updateIndex);
    }
  }
}
