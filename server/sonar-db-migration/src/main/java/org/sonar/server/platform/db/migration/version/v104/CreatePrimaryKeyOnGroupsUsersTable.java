/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddPrimaryKeyBuilder;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.version.v104.AddUuidColumnToGroupsUsers.GROUPS_USERS_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v104.AddUuidColumnToGroupsUsers.GROUPS_USERS_UUID_COLUMN_NAME;

public class CreatePrimaryKeyOnGroupsUsersTable extends DdlChange {

  @VisibleForTesting
  static final String PK_NAME = "pk_groups_users";

  public CreatePrimaryKeyOnGroupsUsersTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    createPrimaryKey(context);
  }

  private void createPrimaryKey(Context context) throws SQLException {
    boolean pkExists = new DbPrimaryKeyConstraintFinder(getDatabase()).findConstraintName(GROUPS_USERS_TABLE_NAME).isPresent();
    if (!pkExists) {
      context.execute(new AddPrimaryKeyBuilder(GROUPS_USERS_TABLE_NAME, GROUPS_USERS_UUID_COLUMN_NAME).build());
    }
  }
}
