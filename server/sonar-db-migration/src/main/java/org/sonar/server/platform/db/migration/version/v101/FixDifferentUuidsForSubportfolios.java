/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class FixDifferentUuidsForSubportfolios extends DataChange {
  private static final String SELECT_QUERY = """
    SELECT p.uuid, c.uuid
    FROM portfolios p
    INNER join components c on p.kee = c.kee AND p.uuid != c.uuid
    and p.parent_uuid IS NOT NULL and p.root_uuid = c.branch_uuid and c.qualifier = 'SVW'
    """;

  public FixDifferentUuidsForSubportfolios(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      MassUpdate massUpdate = context.prepareMassUpdate();
      massUpdate.select(SELECT_QUERY);
      massUpdate.update("update portfolios set parent_uuid=? where parent_uuid=?");
      massUpdate.update("update portfolios set uuid=? where uuid=?");
      massUpdate.update("update portfolio_projects set portfolio_uuid=? where portfolio_uuid=?");
      massUpdate.update("update portfolio_references set portfolio_uuid=? where portfolio_uuid=?");

      massUpdate.execute((row, update, index) -> {
        String portfolioUuid = row.getString(1);
        String componentUuid = row.getString(2);
        update.setString(1, componentUuid);
        update.setString(2, portfolioUuid);
        return true;
      });
    }
  }
}
