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
package org.sonar.db.version.v54;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

/**
 * Global permission 'profileadmin' is split into 'profileadmin' and 'gateadmin' so for each permission in DB
 * 'profileadmin', a permission 'gateadmin' must be inserted in DB to keep the same level as before this split is
 * introduced in SQ.
 */
public class InsertGateAdminPermissionForEachProfileAdmin extends BaseDataChange {

  public InsertGateAdminPermissionForEachProfileAdmin(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    updateGroupAnyOne(context);
    updateOtherGroups(context);
    updateUsers(context);
  }

  private void updateGroupAnyOne(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("Group AnyOne");
    update.select("select gr1.id from group_roles gr1 " +
      "where gr1.role = 'profileadmin' " +
      "and gr1.resource_id is null " +
      "and gr1.group_id is null " +
      "and not exists (" +
      " select gr2.id from group_roles gr2 " +
      " where gr2.group_id is null " +
      " and gr2.resource_id is null " +
      " and gr2.role='gateadmin'" +
      ")");
    update.update("insert into group_roles " +
      "(group_id,resource_id,role) " +
      "values " +
      "(null, null, 'gateadmin')");
    update.execute(GroupAnyOneHandler.INSTANCE);
  }

  private void updateOtherGroups(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("Other groups");
    update.select("select gr1.group_id from group_roles gr1 " +
      "where gr1.role = 'profileadmin' " +
      "and gr1.resource_id is null " +
      "and gr1.group_id is not null " +
      "and not exists (" +
      " select gr2.id from group_roles gr2 " +
      " where gr2.group_id=gr1.group_id " +
      " and gr2.resource_id is null " +
      " and gr2.role='gateadmin'" +
      ")");
    update.update("insert into group_roles " +
      "(group_id,resource_id,role) " +
      "values " +
      "(?, null, 'gateadmin')");
    update.execute(OtherGroupsHandler.INSTANCE);
  }

  private void updateUsers(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("Users");
    update.select("select ur1.user_id from user_roles ur1 " +
      "where ur1.role = 'profileadmin' " +
      "and ur1.resource_id is null " +
      "and not exists (" +
      " select ur2.id from user_roles ur2 " +
      " where ur2.user_id=ur1.user_id " +
      " and ur2.resource_id is null " +
      " and ur2.role='gateadmin'" +
      ")");
    update.update("insert into user_roles " +
      "(user_id,resource_id,role) " +
      "values " +
      "(?,null,'gateadmin')");
    update.execute(UserRolesHandler.INSTANCE);
  }

  private enum GroupAnyOneHandler implements MassUpdate.Handler {
    INSTANCE;

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      return true;
    }
  }

  private enum OtherGroupsHandler implements MassUpdate.Handler {
    INSTANCE;

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      update.setLong(1, row.getLong(1));
      return true;
    }
  }

  private enum UserRolesHandler implements MassUpdate.Handler {
    INSTANCE;

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      update.setLong(1, row.getLong(1));
      return true;
    }
  }
}
