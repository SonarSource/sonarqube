/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v82;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.rules.RuleType.VULNERABILITY;

public class MigrateManualVulnerabilitiesToSecurityHotSpots extends DataChange {
  private System2 system;

  public MigrateManualVulnerabilitiesToSecurityHotSpots(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate updateIssues = context.prepareMassUpdate();
    updateIssues.select("select id, kee, project_uuid, component_uuid from issues where from_hotspot = ? and issue_type = ?")
      .setBoolean(1, true)
      .setInt(2, 3);
    updateIssues.update("update issues set issue_type = ?, status = ? where id = ? and from_hotspot = ? and issue_type = ?");
    updateIssues.update("insert into issue_changes(issue_key, change_type, change_data, created_at, updated_at, issue_change_creation_date) " +
      "VALUES(?, ?, ?, ?, ?, ?)");

    updateIssues.execute((row, update, updateIndex) -> {
      if (updateIndex == 0) {
        update.setInt(1, 4)
          .setString(2, STATUS_TO_REVIEW)
          .setLong(3, row.getLong(1))
          .setBoolean(4, true)
          .setInt(5, VULNERABILITY.getDbConstant());
      } else if (updateIndex == 1) {
        long currentTime = system.now();
        update.setString(1, row.getString(2))
          .setString(2, "diff")
          .setString(3, "type=VULNERABILITY|SECURITY_HOTSPOT,status=OPEN|TO_REVIEW")
          .setLong(4, currentTime)
          .setLong(5, currentTime)
          .setLong(6, currentTime);
      }
      return true;
    });
  }
}
