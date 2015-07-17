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

package org.sonar.db.version.v51;

import java.sql.SQLException;
import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

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
    massUpdate.select("SELECT i.created_at, i.updated_at, i.issue_change_creation_date, i.id FROM issue_changes i WHERE created_at_ms IS NULL");
    massUpdate.update("UPDATE issue_changes SET created_at_ms=?, updated_at_ms=?, issue_change_creation_date_ms=? WHERE id=?");
    massUpdate.rowPluralName("issue_changes");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Date createdAt = row.getNullableDate(1);
        Date updatedAt = row.getNullableDate(2);
        Date functionalCreatedAt = row.getNullableDate(3);
        Long id = row.getNullableLong(4);

        update.setLong(1, createdAt == null ? now : Math.min(now, createdAt.getTime()));
        update.setLong(2, updatedAt == null ? now : Math.min(now, updatedAt.getTime()));
        update.setLong(3, functionalCreatedAt == null ? null : functionalCreatedAt.getTime());
        update.setLong(4, id);
        return true;
      }
    });
  }

}
