/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class RemoveReportProperties extends DataChange {
  private static final String SELECT_QUERY = """
    SELECT p.uuid as uuid from properties p
    WHERE  p.prop_key = 'sonar.governance.report.lastSendTimeInMs' or  p.prop_key = 'sonar.governance.report.project.branch.lastSendTimeInMs'
    or p.prop_key = 'sonar.governance.report.userNotification' or p.prop_key = 'sonar.governance.report.project.branch.userNotification'
    """;

  public RemoveReportProperties(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      MassUpdate massUpdate = context.prepareMassUpdate();
      massUpdate.select(SELECT_QUERY);
      massUpdate.update("delete from properties where uuid = ?");
      massUpdate.execute((row, delete) -> {
        String uuid = row.getString(1);
        delete.setString(1, uuid);
        return true;
      });
    }
  }
}
