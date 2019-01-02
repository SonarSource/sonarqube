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

public class CleanOrphanRowsInSnapshots extends DataChange {

  public CleanOrphanRowsInSnapshots(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT sn.id, sn.project_id, sn.root_project_id from snapshots sn where sn.component_uuid is null or sn.root_component_uuid is null");
    massUpdate.update("DELETE from duplications_index WHERE snapshot_id=? or project_snapshot_id=?");
    massUpdate.update("DELETE from project_measures WHERE snapshot_id=?");
    massUpdate.update("DELETE from ce_activity WHERE snapshot_id=?");
    massUpdate.update("DELETE from events WHERE snapshot_id=?");
    massUpdate.update("DELETE from snapshots WHERE id=?");
    massUpdate.rowPluralName("snapshots");
    massUpdate.execute((row, update, updateIndex) -> {
      long snapshotId = row.getLong(1);
      switch (updateIndex) {
        case 0:
          update.setLong(1, snapshotId);
          update.setLong(2, snapshotId);
          return true;
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
