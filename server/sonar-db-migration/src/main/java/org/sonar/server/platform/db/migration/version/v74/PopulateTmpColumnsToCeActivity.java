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
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

@SupportsBlueGreen
public class PopulateTmpColumnsToCeActivity extends DataChange {

  public PopulateTmpColumnsToCeActivity(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    // activity of long and short branches
    populateCeActivityTmpColumns(context, "Archived tasks of branches",
      "select" +
        "  cea.uuid, p.uuid, cea.component_uuid" +
        " from ce_activity cea" +
        " inner join projects mp on mp.uuid = cea.component_uuid" +
        " inner join ce_task_characteristics ctc1 on ctc1.task_uuid = cea.uuid and ctc1.kee = 'branchType'" +
        " inner join ce_task_characteristics ctc2 on ctc2.task_uuid = cea.uuid and ctc2.kee = 'branch'" +
        " inner join projects p on p.kee = concat(mp.kee, concat(':BRANCH:', ctc2.text_value))" +
        " where" +
        "  cea.component_uuid is not null" +
        "  and (cea.tmp_component_uuid is null or cea.tmp_main_component_uuid is null)");

    // activity of PRs
    populateCeActivityTmpColumns(context, "Archived tasks of PRs",
      "select" +
        "  cea.uuid, p.uuid, cea.component_uuid" +
        " from ce_activity cea" +
        " inner join projects mp on mp.uuid = cea.component_uuid " +
        " inner join ce_task_characteristics ctc1 on ctc1.task_uuid = cea.uuid and ctc1.kee = 'pullRequest'" +
        " inner join projects p on p.kee = concat(mp.kee, concat(':PULL_REQUEST:', ctc1.text_value))" +
        " where" +
        "  cea.component_uuid is not null" +
        "  and (cea.tmp_component_uuid is null or cea.tmp_main_component_uuid is null)");

    // all activities which tmp columns are not populated yet (will include main and deprecated branches)
    // both tmp columns will be set to CE_ACTIVITY.COMPONENT_UUID
    // do not join on PROJECTS to also catch orphans
    populateCeActivityTmpColumns(context, "Archived tasks of main and deprecated branches",
      "select" +
        "  cea.uuid, cea.component_uuid, cea.component_uuid" +
        " from ce_activity cea" +
        " where" +
        "  cea.component_uuid is not null" +
        "  and (cea.tmp_component_uuid is null or cea.tmp_main_component_uuid is null)");
  }

  private static void populateCeActivityTmpColumns(Context context, String rowPluralName, String sql) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(sql);
    massUpdate.update("update ce_activity set tmp_component_uuid=?, tmp_main_component_uuid=? where uuid=?");
    massUpdate.rowPluralName(rowPluralName);
    massUpdate.execute(PopulateTmpColumnsToCeActivity::handleUpdate);
  }

  private static boolean handleUpdate(Select.Row row, SqlStatement update) throws SQLException {
    String uuid = row.getString(1);
    String componentUuuid = row.getString(2);
    String mainComponentUuuid = row.getString(3);

    update.setString(1, componentUuuid);
    update.setString(2, mainComponentUuuid);
    update.setString(3, uuid);

    return true;
  }
}
