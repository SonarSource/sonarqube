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

public class PurgeComponentsWithoutProjectUuid extends DataChange {
  public PurgeComponentsWithoutProjectUuid(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id,uuid from projects where project_uuid is null");
    massUpdate.rowPluralName("rows in projects without project_uuid");
    massUpdate.update("delete from duplications_index where analysis_uuid in (select uuid from snapshots where component_uuid=?)");
    massUpdate.update("delete from project_measures where component_uuid=?");
    massUpdate.update("delete from ce_activity where component_uuid=?");
    massUpdate.update("delete from events where component_uuid=?");
    massUpdate.update("delete from events where analysis_uuid in (select uuid from snapshots where component_uuid=?)");
    massUpdate.update("delete from project_links where component_uuid=?");
    massUpdate.update("delete from snapshots where component_uuid=?");
    massUpdate.update("delete from issues where component_uuid=? or project_uuid=?");
    massUpdate.update("delete from file_sources where file_uuid=? or project_uuid=?");
    massUpdate.update("delete from group_roles where resource_id=?");
    massUpdate.update("delete from user_roles where resource_id=?");
    massUpdate.update("delete from properties where resource_id=?");
    massUpdate.update("delete from projects where uuid=?");
    massUpdate.execute((row, update, updateIndex) -> {
      long componentId = row.getLong(1);
      String componentUuid = row.getString(2);
      switch (updateIndex) {
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
          update.setString(1, componentUuid);
          return true;
        case 7:
        case 8:
          update.setString(1, componentUuid);
          update.setString(2, componentUuid);
          return true;
        case 9:
        case 10:
        case 11:
          update.setLong(1, componentId);
          return true;
        case 12:
          update.setString(1, componentUuid);
          return true;
        default:
          throw new IllegalArgumentException("Unsupported update index " + updateIndex);
      }
    });
  }
}
