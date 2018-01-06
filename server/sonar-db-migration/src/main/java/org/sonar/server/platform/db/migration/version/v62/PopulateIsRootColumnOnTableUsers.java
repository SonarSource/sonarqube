/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v62;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

/**
 * All users with "admin" role, either directly or via a group, are made root. All others are made non root.
 */
public class PopulateIsRootColumnOnTableUsers extends DataChange {

  private static final String ROLE_ADMIN = "admin";

  public PopulateIsRootColumnOnTableUsers(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    String sql = "select distinct ur.user_id as id" +
      " from user_roles ur" +
      " inner join users u on u.id=ur.user_id and u.active=?" +
      " where" +
      " ur.role = ?" +
      " and ur.resource_id is null" +
      " union all" +
      " select distinct u.id as id" +
      " from users u" +
      " inner join groups_users gu on gu.user_id = u.id" +
      " inner join group_roles gr on gr.group_id = gu.group_id" +
      " inner join groups g on g.id = gu.group_id" +
      " where" +
      " gr.role = ?" +
      " and gr.resource_id is null" +
      " and u.active = ?";
    massUpdate.select(sql)
      .setBoolean(1, true)
      .setString(2, ROLE_ADMIN)
      .setString(3, ROLE_ADMIN)
      .setBoolean(4, true);
    massUpdate.update("update users set is_root=? where id = ?");
    massUpdate.rowPluralName("Users with System Administer permission as root");
    massUpdate.execute(PopulateIsRootColumnOnTableUsers::handle);

    context.prepareUpsert("update users set is_root=? where is_root is null")
      .setBoolean(1, false)
      .execute()
      .commit();
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    long id = row.getLong(1);

    update.setBoolean(1, true);
    update.setLong(2, id);
    return true;
  }
}
