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

public class FeedSnapshotsLongDates extends BaseDataChange {

  private final System2 system2;

  public FeedSnapshotsLongDates(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate
      .select("SELECT s.created_at, s.build_date, s.period1_date, s.period2_date, s.period3_date, s.period4_date, s.period5_date, s.id FROM snapshots s " +
        "WHERE created_at_ms IS NULL");
    massUpdate
      .update("UPDATE snapshots " +
        "SET created_at_ms=?, build_date_ms=?, period1_date_ms=?, period2_date_ms=?, period3_date_ms=?, period4_date_ms=?, period5_date_ms=? " +
        "WHERE id=?");
    massUpdate.rowPluralName("snapshots");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        for (int i = 1; i <= 7; i++) {
          Date date = row.getNullableDate(i);
          update.setLong(i, date == null ? null : Math.min(now, date.getTime()));
        }

        Long id = row.getNullableLong(8);
        update.setLong(8, id);

        return true;
      }
    });
  }

}
