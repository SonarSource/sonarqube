/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v52;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

public class FeedManualMeasuresComponentUuid extends BaseDataChange {

  public FeedManualMeasuresComponentUuid(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("manual measures");
    update.select(
      "SELECT p.uuid, mm.resource_id " +
        "FROM manual_measures mm " +
        "INNER JOIN projects p ON mm.resource_id = p.id " +
        "WHERE mm.component_uuid IS NULL");
    update.update("UPDATE manual_measures SET component_uuid=? WHERE resource_id=?");
    update.execute(new SqlRowHandler());
  }

  private static class SqlRowHandler implements MassUpdate.Handler {
    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      update.setString(1, row.getString(1));
      update.setLong(2, row.getLong(2));
      return true;
    }
  }
}
