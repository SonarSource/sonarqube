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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.util.Arrays.asList;

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
    for (String tableName : asList("ce_task_characteristics", "ce_task_input", "ce_scanner_context")) {
      deleteOrphansFrom(context, tableName);
    }
  }

  private void deleteOrphansFrom(Context context, String tableName) throws SQLException {
    String query = buildDeleteFromQuery(tableName, "c",
      "not exists (select 1 from ce_activity ca where ca.uuid = c.task_uuid)" +
        "and not exists (select 1 from ce_queue cq where cq.uuid = c.task_uuid)");

    context.prepareUpsert(query)
      .execute()
      .commit();
  }

  private String buildDeleteFromQuery(String tableName, String alias, String whereClause) {
    String dialectId = getDialect().getId();
    if ("mssql".equals(dialectId) || "mysql".equals(dialectId)) {
      return "delete " + alias + " from " + tableName + " as " + alias + " where " + whereClause;
    }
    return "delete from " + tableName + " " + alias + " where " + whereClause;
  }

}
