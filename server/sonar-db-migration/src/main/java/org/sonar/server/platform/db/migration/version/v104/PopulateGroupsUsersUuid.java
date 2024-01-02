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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.Upsert;

public class PopulateGroupsUsersUuid extends DataChange {

  private static final String SELECT_QUERY = """
      SELECT group_uuid, user_uuid
        FROM groups_users
        WHERE uuid IS NULL
    """;

  private static final String SET_UUID_STATEMENT = """
      UPDATE groups_users
         SET uuid=?
       WHERE group_uuid=? AND user_uuid=?
    """;

  private final UuidFactory uuidFactory;

  public PopulateGroupsUsersUuid(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    SqlStatement<Select> select = massUpdate.select(SELECT_QUERY);
    Upsert setUuid = massUpdate.update(SET_UUID_STATEMENT);
    try (select; setUuid) {
      massUpdate.execute((row, update, index) -> {
        String groupUuid = row.getString(1);
        String userUuid = row.getString(2);
        String uuid = uuidFactory.create();
        update.setString(1, uuid);
        update.setString(2, groupUuid);
        update.setString(3, userUuid);
        return true;
      });
    }
  }
}
