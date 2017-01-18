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
package org.sonar.db.version.v564;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;

public class CleanUsurperRootComponents extends BaseDataChange {

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

  private void fixSnapshotScopeAndQualifier(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(fixSqlConditions(getDb().getDialect(), "select" +
      " sn.id,p.scope,p.qualifier" +
      " from" +
      " snapshots sn, projects p" +
      " where" +
      " p.id = sn.project_id" +
      " and (p.qualifier %s <> sn.qualifier %s or p.scope %s <> sn.scope %s)"));
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

  /**
   * Replace placeholders {@code "%s"} so that comparisons of VARCHAR columns
   * do not fail on MsSQL when column collations are different.
   */
  @VisibleForTesting
  static String fixSqlConditions(Dialect dialect, String sql) {
    if (MsSql.ID.equals(dialect.getId())) {
      return sql.replaceAll("%s", "collate database_default");
    }
    return sql.replaceAll("%s", "");
  }

  private void cleanUsurperRootComponents(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(fixSqlConditions(getDb().getDialect(), "select p.id,p.uuid from projects p " +
      " where" +
      " p.project_uuid %s = p.uuid %s" +
      " and not (" +
      " p.scope = 'PRJ'" +
      " and p.qualifier in ('TRK', 'VW', 'DEV')" +
      " )"));
    massUpdate.update("delete from duplications_index where snapshot_id in (select id from snapshots where project_id=?)");
    massUpdate.update("delete from project_measures where project_id=?");
    massUpdate.update("delete from ce_activity where component_uuid=?");
    massUpdate.update("delete from events where component_uuid=?");
    massUpdate.update("delete from project_links where component_uuid=?");
    massUpdate.update("delete from snapshots where project_id=? or root_project_id=?");
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
          update.setLong(1, componentId);
          return true;
        case 2:
        case 3:
        case 4:
          update.setString(1, componentUuid);
          return true;
        case 5:
          update.setLong(1, componentId);
          update.setLong(2, componentId);
          return true;
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
      " p.id = sn.root_project_id" +
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
