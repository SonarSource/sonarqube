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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.DataChange;

public class PopulateComponentUuidAndAnalysisUuidOfDuplicationsIndex extends DataChange {

  public PopulateComponentUuidAndAnalysisUuidOfDuplicationsIndex(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    populateComponentUuid(context);
    populateAnalysisUuid(context);
  }

  private static void populateComponentUuid(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select distinct di.snapshot_id, s.component_uuid from duplications_index di" +
      " inner join snapshots s on s.id=di.snapshot_id" +
      " where di.component_uuid is null");
    massUpdate.update("UPDATE duplications_index SET component_uuid=? WHERE snapshot_id=? and component_uuid is null");
    massUpdate.rowPluralName("component uuid of duplications_index entries");
    massUpdate.execute(PopulateComponentUuidAndAnalysisUuidOfDuplicationsIndex::handleComponentUuid);
  }

  private static boolean handleComponentUuid(Select.Row row, SqlStatement update) throws SQLException {
    long snapshotId = row.getLong(1);
    String componentUuid = row.getString(2);

    update.setString(1, componentUuid);
    update.setLong(2, snapshotId);

    return true;
  }

  private static void populateAnalysisUuid(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select distinct di.project_snapshot_id, s.uuid from duplications_index di" +
      " inner join snapshots s on s.id=di.project_snapshot_id" +
      " where di.analysis_uuid is null");
    massUpdate.update("UPDATE duplications_index SET analysis_uuid=? WHERE project_snapshot_id=? and analysis_uuid is null");
    massUpdate.rowPluralName("analysis uuid of duplications_index entries");
    massUpdate.execute(PopulateComponentUuidAndAnalysisUuidOfDuplicationsIndex::handleAnalysisUuid);
  }

  private static boolean handleAnalysisUuid(Select.Row row, SqlStatement update) throws SQLException {
    long projectSnapshotId = row.getLong(1);
    String snapshotUuid = row.getString(2);

    update.setString(1, snapshotUuid);
    update.setLong(2, projectSnapshotId);

    return true;
  }

}
