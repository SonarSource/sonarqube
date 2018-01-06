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
import org.sonar.server.platform.db.migration.step.DataChange;

public class DeleteOrphanDuplicationsIndexRowsWithoutComponentOrAnalysis extends DataChange {

  public DeleteOrphanDuplicationsIndexRowsWithoutComponentOrAnalysis(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    deleteRowsWithoutComponentUuid(context);
    deleteRowsWithoutAnalysisUuid(context);
  }

  private static void deleteRowsWithoutComponentUuid(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT id from duplications_index where component_uuid is null");
    massUpdate.update("DELETE from duplications_index WHERE id=?");
    massUpdate.rowPluralName("duplications index rows without component");
    massUpdate.execute((row, update) -> {
      update.setLong(1, row.getLong(1));
      return true;
    });
  }

  private static void deleteRowsWithoutAnalysisUuid(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT distinct project_snapshot_id from duplications_index where analysis_uuid is null");
    massUpdate.update("DELETE from duplications_index WHERE project_snapshot_id=?");
    massUpdate.rowPluralName("duplications index rows without analysis");
    massUpdate.execute((row, update) -> {
      update.setLong(1, row.getLong(1));
      return true;
    });
  }

}
