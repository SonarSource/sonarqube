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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;

/**
 * The migration drops the orphans from tables ce_*. It can be executed
 * when server is up, so it supports blue/green deployments.
 */
@SupportsBlueGreen
public class PurgeOrphansForCE extends DataChange {

  public PurgeOrphansForCE(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    switch (getDialect().getId()) {
      case "mssql":
      case "mysql":
        executeForMySQLAndMsSQL(context);
        break;
      default:
        executeGeneric(context);
        break;
    }
  }

  private static void executeGeneric(Context context) throws SQLException {
    context.prepareUpsert("delete from ce_task_characteristics ctc where not exists (select 1 from ce_activity ca where ca.uuid = ctc.task_uuid)")
      .execute()
      .commit();

    context.prepareUpsert("delete from ce_task_input cti where not exists (select 1 from ce_activity ca where ca.uuid = cti.task_uuid)")
      .execute()
      .commit();

    context.prepareUpsert("delete from ce_scanner_context csc where not exists (select 1 from ce_activity ca where ca.uuid = csc.task_uuid)")
      .execute()
      .commit();
  }

  private static void executeForMySQLAndMsSQL(Context context) throws SQLException {
    context.prepareUpsert("delete ctc from ce_task_characteristics as ctc where not exists (select 1 from ce_activity ca where ca.uuid = ctc.task_uuid)")
      .execute()
      .commit();

    context.prepareUpsert("delete cti from ce_task_input as cti where not exists (select 1 from ce_activity ca where ca.uuid = cti.task_uuid)")
      .execute()
      .commit();

    context.prepareUpsert("delete csc from ce_scanner_context as csc where not exists (select 1 from ce_activity ca where ca.uuid = csc.task_uuid)")
      .execute()
      .commit();

  }
}
