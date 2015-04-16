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

package org.sonar.server.db.migrations.v52;

import java.sql.SQLException;

import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.MassUpdate.Handler;
import org.sonar.server.db.migrations.Select.Row;
import org.sonar.server.db.migrations.SqlStatement;

public class FeedProjectLinksComponentUuid extends BaseDataChange {

  public FeedProjectLinksComponentUuid(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("project links");
    update.select(
      "SELECT p.uuid, link.id " +
        "FROM project_links link " +
        "INNER JOIN projects p ON p.id=link.project_id " +
        "WHERE link.component_uuid is null");
    update.update("UPDATE project_links SET component_uuid=? WHERE id=?");
    update.execute(new Handler() {
      @Override
      public boolean handle(Row row, SqlStatement update) throws SQLException {
        update.setString(1, row.getString(1));
        update.setLong(2, row.getLong(2));
        return true;
      }
    });
  }
}
