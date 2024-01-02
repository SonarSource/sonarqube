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
package org.sonar.server.platform.db.migration.version.v96;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class DeleteDuplicatedProjectBadgeTokens extends DataChange {

  private static final String DELETE_ITEMS_WITH_DIFFERENT_CREATION_DATES = "delete from project_badge_token where exists ("
    + " select 1 from project_badge_token b where project_badge_token.project_uuid = b.project_uuid and project_badge_token.created_at > b.created_at)";
  private static final String DELETE_ITEMS_WITH_SAME_CREATION_DATES = "delete from project_badge_token where exists ("
    + " select 1 from project_badge_token b where project_badge_token.project_uuid = b.project_uuid and b.uuid < project_badge_token.uuid)";

  public DeleteDuplicatedProjectBadgeTokens(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    executeQuery(context, DELETE_ITEMS_WITH_DIFFERENT_CREATION_DATES);
    executeQuery(context, DELETE_ITEMS_WITH_SAME_CREATION_DATES);
  }

  private static void executeQuery(Context context, String sql) throws SQLException {
    Upsert upsert = context.prepareUpsert(sql);
    upsert.execute();
    upsert.commit();
  }
}
