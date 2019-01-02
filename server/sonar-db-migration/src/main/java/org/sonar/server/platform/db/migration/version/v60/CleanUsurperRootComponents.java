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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.DataChange;

public class CleanUsurperRootComponents extends DataChange {

  public CleanUsurperRootComponents(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    // fix snapshots which don't have the scope and/or qualifier of their associated component
    fixSnapshotScopeAndQualifier(context);
    // delete components declaring themselves as root in table PROJECTS but which don't have a root scope and/or qualifier
    cleanUsurperRootComponents(context);
    // components which has snapshots reference a component as root which is not a root
    cleanSnapshotWithIncorrectRoot(context);
  }

  private static void fixSnapshotScopeAndQualifier(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      " sn.id,p.scope,p.qualifier" +
      " from" +
      " snapshots sn, projects p" +
      " where" +
      " p.uuid = sn.component_uuid" +
      " and (p.qualifier<>sn.qualifier or p.scope<>sn.scope)");
    massUpdate.update("update snapshots set scope=?,qualifier=? where id=?");
    massUpdate.rowPluralName("snapshots with inconsistent scope or qualifier");
    massUpdate.execute((row, update) -> {
      long snapshotId = row.getLong(1);
      String scope = row.getString(2);
      String qualifier = row.getString(3);

      update.setString(1, scope);
      update.setString(2, qualifier);
      update.setLong(3, snapshotId);

      return true;
    });
  }

  private static void cleanUsurperRootComponents(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select p.id,p.uuid from projects p " +
      " where" +
      " p.project_uuid = p.uuid" +
      " and not (" +
      " p.scope = 'PRJ'" +
      " and p.qualifier in ('TRK', 'VW', 'DEV')" +
      " )");
    massUpdate.update("delete from duplications_index where snapshot_id in (select id from snapshots where component_uuid=?)");
    massUpdate.update("delete from project_measures where component_uuid=?");
    massUpdate.update("delete from ce_activity where component_uuid=?");
    massUpdate.update("delete from events where component_uuid=?");
    massUpdate.update("delete from project_links where component_uuid=?");
    massUpdate.update("delete from snapshots where component_uuid=? or root_component_uuid=?");
    massUpdate.update("delete from issues where component_uuid=? or project_uuid=?");
    massUpdate.update("delete from file_sources where file_uuid=? or project_uuid=?");
    massUpdate.update("delete from group_roles where resource_id=?");
    massUpdate.update("delete from user_roles where resource_id=?");
    massUpdate.update("delete from properties where resource_id=?");
    massUpdate.update("delete from widgets where resource_id=?");
    massUpdate.update("delete from projects where uuid=?");
    massUpdate.rowPluralName("usurper root components");
    massUpdate.execute((row, update, updateIndex) -> {
      long componentId = row.getLong(1);
      String componentUuid = row.getString(2);
      switch (updateIndex) {
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
          update.setString(1, componentUuid);
          return true;
        case 5:
        case 6:
        case 7:
          update.setString(1, componentUuid);
          update.setString(2, componentUuid);
          return true;
        case 8:
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

  private static void cleanSnapshotWithIncorrectRoot(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      " sn.id" +
      " from " +
      " projects p, snapshots sn" +
      " where" +
      " p.uuid = sn.root_component_uuid" +
      " and not (" +
      " p.scope = 'PRJ'" +
      " and p.qualifier in ('TRK', 'VW', 'DEV')" +
      " )");
    massUpdate.update("DELETE from ce_activity WHERE snapshot_id=?");
    massUpdate.update("DELETE from events WHERE snapshot_id=?");
    massUpdate.update("DELETE from project_measures WHERE snapshot_id=?");
    massUpdate.update("DELETE from duplications_index WHERE project_snapshot_id=?");
    massUpdate.update("DELETE from snapshots WHERE id=?");
    massUpdate.rowPluralName("snapshots with incorrect root");
    massUpdate.execute((row, update, updateIndex) -> {
      long snapshotId = row.getLong(1);
      switch (updateIndex) {
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
          update.setLong(1, snapshotId);
          return true;
        default:
          throw new IllegalArgumentException("Unsupported update index " + updateIndex);
      }
    });
  }

}
