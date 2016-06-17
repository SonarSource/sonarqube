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
package org.sonar.db.version.v60;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

public class PopulateAnalysisUuidOfDuplicationsIndex extends BaseDataChange {

  public PopulateAnalysisUuidOfDuplicationsIndex(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select distinct di.project_snapshot_id, s.uuid from duplications_index di" +
      " inner join snapshots s on s.id=di.project_snapshot_id" +
      " where di.analysis_uuid is null");
    massUpdate.update("UPDATE duplications_index SET analysis_uuid=? WHERE project_snapshot_id=? and analysis_uuid is null");
    massUpdate.rowPluralName("analysis uuid of duplications_index entries");
    massUpdate.execute(PopulateAnalysisUuidOfDuplicationsIndex::handle);
  }

  public static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    long projectSnapshotId = row.getLong(1);
    String snapshotUuid = row.getString(2);

    update.setString(1, snapshotUuid);
    update.setLong(2, projectSnapshotId);

    return true;
  }

}
