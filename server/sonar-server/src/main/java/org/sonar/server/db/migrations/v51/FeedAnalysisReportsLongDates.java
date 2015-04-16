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

public class FeedAnalysisReportsLongDates extends BaseDataChange {

  private final System2 system;

  public FeedAnalysisReportsLongDates(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final long now = system.now();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT a.created_at, a.updated_at, a.started_at, a.finished_at, a.id FROM analysis_reports a WHERE created_at_ms IS NULL");
    massUpdate.update("UPDATE analysis_reports SET created_at_ms=?, updated_at_ms=?, started_at_ms=?, finished_at_ms=? WHERE id=?");
    massUpdate.rowPluralName("analysis_reports");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Date createdAt = row.getNullableDate(1);
        Date updatedAt = row.getNullableDate(2);
        Date startedAt = row.getNullableDate(3);
        Date finishedAt = row.getNullableDate(4);
        Long id = row.getNullableLong(5);

        update.setLong(1, createdAt == null ? now : Math.min(now, createdAt.getTime()));
        update.setLong(2, updatedAt == null ? now : Math.min(now, updatedAt.getTime()));
        update.setLong(3, startedAt == null ? null : startedAt.getTime());
        update.setLong(4, finishedAt == null ? null : finishedAt.getTime());
        update.setLong(5, id);
        return true;
      }
    });
  }

}
