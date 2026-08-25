/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

/**
 * Permission lookups filter 'group_roles' by (entity_uuid, role) and then need 'group_uuid' to resolve group membership,
 * see AuthorizationMapper#keepAuthorizedEntityUuidsForUser. On large deployments (100k+ projects, millions of
 * 'group_roles' rows) the pre-existing single-column index on 'entity_uuid' was not selective enough and the database
 * fell back to a full scan of 'group_roles' for every batch of entities. This composite index covers the whole
 * condition.
 */
public class CreateIndexOnGroupRolesEntityRoleGroup extends DdlChange {

  static final String TABLE_NAME = "group_roles";
  static final String INDEX_NAME = "group_roles_ent_role_grp";
  static final String ENTITY_UUID_COLUMN_NAME = "entity_uuid";
  static final String ROLE_COLUMN_NAME = "role";
  static final String GROUP_UUID_COLUMN_NAME = "group_uuid";

  public CreateIndexOnGroupRolesEntityRoleGroup(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  private void createIndex(Context context, Connection connection) {
    if (!DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName(INDEX_NAME)
        .addColumn(ENTITY_UUID_COLUMN_NAME, true)
        .addColumn(ROLE_COLUMN_NAME, false)
        .addColumn(GROUP_UUID_COLUMN_NAME, true)
        .build());
    }
  }
}
