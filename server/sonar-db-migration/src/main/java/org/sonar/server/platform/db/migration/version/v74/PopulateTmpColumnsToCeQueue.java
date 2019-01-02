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
public class PopulateTmpColumnsToCeQueue extends DataChange {
  public PopulateTmpColumnsToCeQueue(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    // queued task of long and short branches which have already been analyzed at least once
    populateCeQueueTmpColumns(context, "queued tasks of branches",
      "select" +
        "  cq.uuid, p.uuid, cq.component_uuid" +
        " from ce_queue cq" +
        " inner join projects mp on mp.uuid = cq.component_uuid" +
        " inner join ce_task_characteristics ctc1 on ctc1.task_uuid = cq.uuid and ctc1.kee = 'branchType'" +
        " inner join ce_task_characteristics ctc2 on ctc2.task_uuid = cq.uuid and ctc2.kee = 'branch'" +
        " inner join projects p on p.kee = concat(mp.kee, concat(':BRANCH:', ctc2.text_value))" +
        " where" +
        "  cq.component_uuid is not null" +
        "  and (cq.tmp_component_uuid is null or cq.tmp_main_component_uuid is null)");

    // queued task of pull request which have already been analyzed at least once
    populateCeQueueTmpColumns(context, "queued tasks of PRs",
      " select" +
        "  cq.uuid, p.uuid, cq.component_uuid" +
        " from ce_queue cq" +
        " inner join projects mp on mp.uuid = cq.component_uuid " +
        " inner join ce_task_characteristics ctc1 on ctc1.task_uuid = cq.uuid and ctc1.kee = 'pullRequest'" +
        " inner join projects p on p.kee = concat(mp.kee, concat(':PULL_REQUEST:', ctc1.text_value))" +
        " where" +
        "  cq.component_uuid is not null" +
        "  and (cq.tmp_component_uuid is null or cq.tmp_main_component_uuid is null)");

    // queued task of long and short branches which have never been analyzed must be deleted
    deleteFromCeQueue(context, "queued tasks of never analyzed branches",
      "select" +
        "  cq.uuid" +
        " from ce_queue cq" +
        " inner join projects mp on mp.uuid = cq.component_uuid" +
        " inner join ce_task_characteristics ctc1 on ctc1.task_uuid = cq.uuid and ctc1.kee = 'branchType'" +
        " inner join ce_task_characteristics ctc2 on ctc2.task_uuid = cq.uuid and ctc2.kee = 'branch'" +
        " where" +
        "  cq.component_uuid is not null" +
        "  and (cq.tmp_component_uuid is null or cq.tmp_main_component_uuid is null)" +
        "  and not exists (select 1 from projects p where p.kee = concat(mp.kee, concat(':BRANCH:', ctc2.text_value)))");

    // queued of pull request which have never been analyzed must be deleted
    deleteFromCeQueue(context, "queued tasks of never analyzed PRs",
      "select" +
        "  cq.uuid" +
        " from ce_queue cq" +
        " inner join projects mp on mp.uuid = cq.component_uuid " +
        " inner join ce_task_characteristics ctc1 on ctc1.task_uuid = cq.uuid and ctc1.kee = 'pullRequest'" +
        " where" +
        "  cq.component_uuid is not null" +
        "  and (cq.tmp_component_uuid is null or cq.tmp_main_component_uuid is null)" +
        "  and not exists (select 1 from projects p where p.kee = concat(mp.kee, concat(':PULL_REQUEST:', ctc1.text_value)))");

    // all queue which tmp columns are not populated yet (will include main and deprecated branches)
    // both tmp columns will be set to CE_QUEUE.COMPONENT_UUID
    // do not join on PROJECTS to also catch orphans (there are many for branch and PRs due to SONAR-10642)
    populateCeQueueTmpColumns(context, "queued tasks of main and deprecated branches",
      "select" +
        "  cq.uuid, cq.component_uuid, cq.component_uuid" +
        " from ce_queue cq" +
        " where" +
        " cq.component_uuid is not null" +
        " and (cq.tmp_component_uuid is null or cq.tmp_main_component_uuid is null)");
  }

  private static void populateCeQueueTmpColumns(Context context, String pluralName, String selectSQL) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(selectSQL);
    massUpdate.update("update ce_queue set tmp_component_uuid=?, tmp_main_component_uuid=? where uuid=?");
    massUpdate.rowPluralName(pluralName);
    massUpdate.execute(PopulateTmpColumnsToCeQueue::handleUpdate);
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

  private static void deleteFromCeQueue(Context context, String pluralName, String sql) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(sql);
    massUpdate.update("delete from ce_queue where uuid = ?");
    massUpdate.rowPluralName(pluralName);
    massUpdate.execute(PopulateTmpColumnsToCeQueue::handleDelete);
  }

  private static boolean handleDelete(Select.Row row, SqlStatement update) throws SQLException {
    String uuid = row.getString(1);

    update.setString(1, uuid);

    return true;
  }
}
