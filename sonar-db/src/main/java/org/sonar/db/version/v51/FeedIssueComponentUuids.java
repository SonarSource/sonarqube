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
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.MassUpdate.Handler;
import org.sonar.db.version.Select.Row;
import org.sonar.db.version.SqlStatement;

public class FeedIssueComponentUuids extends BaseDataChange {

  public FeedIssueComponentUuids(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("issues");
    update.select(
      "SELECT c.uuid, c.project_uuid, i.id " +
        "FROM issues i " +
        "INNER JOIN projects c ON i.component_id=c.id " +
        "WHERE i.component_uuid is null");
    update.update("UPDATE issues SET component_uuid=?, project_uuid=? WHERE id=?");
    update.execute(new SqlRowHandler());
  }

  private static class SqlRowHandler implements Handler {
    @Override
    public boolean handle(Row row, SqlStatement update) throws SQLException {
      update.setString(1, row.getNullableString(1));
      update.setString(2, row.getNullableString(2));
      update.setLong(3, row.getNullableLong(3));

      return true;
    }
  }
}
