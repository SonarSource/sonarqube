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

public class PopulateAnalysisUuidOnEvents extends DataChange {

  public PopulateAnalysisUuidOnEvents(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT distinct e.snapshot_id, s.uuid from events e" +
      " inner join snapshots s on s.id=e.snapshot_id and s.scope = 'PRJ' and s.qualifier in ('TRK', 'DEV', 'VW')" +
      " where e.snapshot_id is not null and e.analysis_uuid is null");
    massUpdate.update("UPDATE events SET analysis_uuid=? WHERE snapshot_id=? and analysis_uuid is null");
    massUpdate.rowPluralName("analysis uuid of root component events");
    massUpdate.execute(PopulateAnalysisUuidOnEvents::handle);
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    long snapshotId = row.getLong(1);
    String snapshotUuid = row.getString(2);

    update.setString(1, snapshotUuid);
    update.setLong(2, snapshotId);

    return true;
  }
}
