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

package org.sonar.server.db.migrations.v43;

import java.sql.SQLException;
import java.util.Date;

import org.sonar.api.issue.Issue;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

/**
 * Used in the Active Record Migration 525
 *
 * @since 4.3
 */
public class NotResolvedIssuesOnRemovedComponentsMigrationStep extends BaseDataChange {

  private final System2 system2;

  public NotResolvedIssuesOnRemovedComponentsMigrationStep(Database database, System2 system2) {
    super(database);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final Date now = new Date(system2.now());
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT i.id FROM issues i " +
      "INNER JOIN projects p on p.id=i.component_id " +
      "WHERE p.enabled=? AND i.resolution IS NULL ").setBoolean(1, false);
    massUpdate.update("UPDATE issues SET status=?,resolution=?,updated_at=? WHERE id=?");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long id = row.getNullableLong(1);
        update.setString(1, Issue.STATUS_CLOSED);
        update.setString(2, Issue.RESOLUTION_REMOVED);
        update.setDate(3, now);
        update.setLong(4, id);
        return true;
      }
    });
  }
}
