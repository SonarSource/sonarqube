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

public class DropSecurityHotSpotsInReviewStatus extends DataChange {

  private System2 system;

  public DropSecurityHotSpotsInReviewStatus(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id,kee from issues where status = 'IN_REVIEW'");
    massUpdate.update("update issues set status = 'TO_REVIEW' where id = ? and status = 'IN_REVIEW'");
    massUpdate.update("insert into issue_changes(issue_key, change_type, change_data, created_at, updated_at, issue_change_creation_date) " +
      "VALUES(?, 'diff', 'status=IN_REVIEW|TO_REVIEW', ?, ?, ?)");
    massUpdate.execute((row, update, updateIndex) -> {

      if (updateIndex == 0) {
        update.setLong(1, row.getLong(1));
      } else if (updateIndex == 1) {
        long currentTime = system.now();
        update.setString(1, row.getString(2))
          .setLong(2, currentTime)
          .setLong(3, currentTime)
          .setLong(4, currentTime);
      }
      return true;
    });
  }
}
