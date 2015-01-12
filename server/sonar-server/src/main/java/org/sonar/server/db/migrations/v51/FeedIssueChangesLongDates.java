/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.db.migrations.v51;

import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

import java.sql.SQLException;
import java.util.Date;

public class FeedIssueChangesLongDates extends BaseDataChange {

  private final System2 system;

  public FeedIssueChangesLongDates(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final long now = system.now();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT i.created_at, i.updated_at, i.issue_change_creation_date_ms, i.id FROM issue_changes i WHERE created_at_ms IS NULL");
    massUpdate.update("UPDATE issue_changes SET created_at_ms=?, updated_at_ms=?, issue_change_creation_date_ms=? WHERE id=?");
    massUpdate.rowPluralName("issue_changes");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Date createdAt = row.getDate(1);
        Date updatedAt = row.getDate(2);
        Date functionalCreatedAt = row.getDate(3);
        Long id = row.getLong(4);

        updateColumn(update, 1, createdAt);
        updateColumn(update, 2, updatedAt);
        if (functionalCreatedAt == null) {
          update.setLong(3, null);
        } else {
          update.setLong(3, functionalCreatedAt.getTime());
        }
        update.setLong(4, id);
        return true;
      }

      private void updateColumn(SqlStatement update, int position, Date time) throws SQLException {
        if (time == null) {
          update.setLong(position, now);
        } else {
          update.setLong(position, Math.min(now, time.getTime()));
        }
      }
    });
  }

}
