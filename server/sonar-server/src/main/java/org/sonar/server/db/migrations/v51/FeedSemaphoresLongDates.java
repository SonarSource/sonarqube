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

import java.sql.SQLException;
import java.util.Date;

import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

public class FeedSemaphoresLongDates extends BaseDataChange {

  private final System2 system2;

  public FeedSemaphoresLongDates(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate
      .select("SELECT s.created_at, s.updated_at, s.locked_at, s.id FROM semaphores s WHERE created_at_ms IS NULL");
    massUpdate
      .update("UPDATE semaphores SET created_at_ms=?, updated_at_ms=?, locked_at_ms=? WHERE id=?");
    massUpdate.rowPluralName("semaphores");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        for (int i = 1; i <= 3; i++) {
          Date date = row.getNullableDate(i);
          update.setLong(i, date == null ? null : Math.min(now, date.getTime()));
        }

        Long id = row.getNullableLong(4);
        update.setLong(4, id);

        return true;
      }
    });
  }

}
