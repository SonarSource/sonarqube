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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class EnsureIssueProjectUuidConsistencyOnIssues extends DataChange {
  public EnsureIssueProjectUuidConsistencyOnIssues(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      " i.id, p.project_uuid" +
      " from issues i" +
      " inner join projects p on" +
      "   p.uuid = i.component_uuid" +
      "   and i.project_uuid <> p.project_uuid");
    massUpdate.update("update issues set project_uuid = ? where id = ?");
    massUpdate.rowPluralName("issues with inconsistent project_uuid");
    massUpdate.execute(EnsureIssueProjectUuidConsistencyOnIssues::handle);
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    long issueId = row.getLong(1);
    String projectUuid = row.getString(2);

    update.setString(1, projectUuid);
    update.setLong(2, issueId);

    return true;
  }
}
