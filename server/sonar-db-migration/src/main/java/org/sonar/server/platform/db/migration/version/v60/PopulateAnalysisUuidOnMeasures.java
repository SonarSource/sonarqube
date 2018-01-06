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
import java.util.HashMap;
import java.util.Map;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class PopulateAnalysisUuidOnMeasures extends DataChange {

  public PopulateAnalysisUuidOnMeasures(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    Map<Long, String> rootSnapshotUuids = loadRootSnapshotUuids(context);


    MassUpdate massUpdate = context.prepareMassUpdate();
    // mysql can take hours if the 2 requests are merged into a single one
    massUpdate.select("select distinct m.snapshot_id as sId, s.root_snapshot_id as rootSid " +
      "from project_measures m " +
      "inner join snapshots s on m.snapshot_id = s.id " +
      "where m.analysis_uuid is null"
    );
    massUpdate.update("update project_measures set analysis_uuid=? where snapshot_id = ? and analysis_uuid is null");
    massUpdate.rowPluralName("measures");
    massUpdate.execute((row, update) -> handleRow(row, update, rootSnapshotUuids));
  }

  private static Map<Long, String> loadRootSnapshotUuids(Context context) throws SQLException {
    Map<Long, String> snapshotUuidsByIds = new HashMap<>();
    context.prepareSelect("select distinct id, uuid from snapshots where depth=0")
      .scroll(row -> snapshotUuidsByIds.put(row.getLong(1), row.getString(2)));
    return snapshotUuidsByIds;
  }

  private static boolean handleRow(Select.Row row, SqlStatement update, Map<Long, String> rootSnapshotUuids) throws SQLException {
    long snapshotId = row.getLong(1);
    Long rootSnapshotId = row.getNullableLong(2);
    String analysisUuid = rootSnapshotUuids.get(rootSnapshotId == null ? snapshotId : rootSnapshotId);
    if (analysisUuid == null) {
      return false;
    }

    update.setString(1, analysisUuid);
    update.setLong(2, snapshotId);
    return true;
  }

}
