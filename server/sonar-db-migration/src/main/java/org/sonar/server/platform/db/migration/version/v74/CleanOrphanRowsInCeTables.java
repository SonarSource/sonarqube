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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.api.config.Configuration;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

@SupportsBlueGreen
public class CleanOrphanRowsInCeTables extends DataChange {
  private final Configuration configuration;

  public CleanOrphanRowsInCeTables(Database db, Configuration configuration1) {
    super(db);
    this.configuration = configuration1;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (isSonarCloud(configuration)) {
      return;
    }

    // clean orphans on ce_activity
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      "  cea.uuid" +
      " from ce_activity cea" +
      " where" +
      "   cea.component_uuid is not null" +
      "   and cea.main_component_uuid is not null" +
      "   and cea.component_uuid = cea.main_component_uuid" +
      "   and exists (select 1 from ce_task_characteristics ctc where ctc.task_uuid = cea.uuid)");
    massUpdate.update("delete from ce_task_input where task_uuid = ?");
    massUpdate.update("delete from ce_scanner_context where task_uuid = ?");
    massUpdate.update("delete from ce_task_characteristics where task_uuid = ?");
    massUpdate.update("delete from ce_activity where uuid = ?");
    massUpdate.rowPluralName("orphans of deleted branch/pr");
    massUpdate.execute((row, update, updateIndex) -> {
      String taskUuid = row.getString(1);
      update.setString(1, taskUuid);
      return true;
    });
  }

}
