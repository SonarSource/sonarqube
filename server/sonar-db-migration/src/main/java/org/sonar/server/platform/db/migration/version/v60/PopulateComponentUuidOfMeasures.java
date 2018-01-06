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

public class PopulateComponentUuidOfMeasures extends DataChange {

  public PopulateComponentUuidOfMeasures(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select distinct pm.snapshot_id, s.component_uuid from project_measures pm inner join snapshots s on s.id=pm.snapshot_id where pm.component_uuid is null");
    massUpdate.update("UPDATE project_measures SET component_uuid=? WHERE snapshot_id=? and component_uuid is null");
    massUpdate.rowPluralName("measures");
    massUpdate.execute(this::handle);
  }

  public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    long snapshotId = row.getLong(1);
    String componentUuid = row.getString(2);

    update.setString(1, componentUuid);
    update.setLong(2, snapshotId);

    return true;
  }

}
