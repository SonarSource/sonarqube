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
package org.sonar.server.platform.db.migration.version.v56;

import java.sql.SQLException;
import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class PopulateInitialSchema extends DataChange {

  private static final String ADMINS_GROUP = "sonar-administrators";
  private static final String USERS_GROUP = "sonar-users";
  private static final String ADMIN_USER = "admin";

  private final System2 system2;

  public PopulateInitialSchema(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    insertGroups(context);
    insertGroupRoles(context);
    insertAdminUser(context);
    insertGroupMemberships(context);

  }

  private void insertGroups(Context context) throws SQLException {
    truncateTable(context, "groups");

    Date now = new Date(system2.now());
    context.prepareUpsert("insert into groups (name, description, created_at, updated_at) values (?, ?, ?, ?)")
      .setString(1, ADMINS_GROUP)
      .setString(2, "System administrators")
      .setDate(3, now)
      .setDate(4, now)
      .addBatch()
      .setString(1, USERS_GROUP)
      .setString(2, "Any new users created will automatically join this group")
      .setDate(3, now)
      .setDate(4, now)
      .addBatch()
      .execute()
      .commit();
  }

  private static void insertGroupRoles(Context context) throws SQLException {
    truncateTable(context, "group_roles");

    // admin group
    context.prepareUpsert("insert into group_roles (group_id, resource_id, role) values ((select id from groups where name='" + ADMINS_GROUP + "'), null, ?)")
      .setString(1, "admin").addBatch()
      .setString(1, "profileadmin").addBatch()
      .setString(1, "gateadmin").addBatch()
      .setString(1, "shareDashboard").addBatch()
      .setString(1, "provisioning").addBatch()
      .execute()
      .commit();

    // anyone
    context.prepareUpsert("insert into group_roles (group_id, resource_id, role) values (null, null, ?)")
      .setString(1, "scan").addBatch()
      .setString(1, "provisioning").addBatch()
      .execute()
      .commit();
  }

  private void insertAdminUser(Context context) throws SQLException {
    truncateTable(context, "users");

    long now = system2.now();
    context.prepareUpsert("insert into users " +
      "(login, name, email, external_identity, external_identity_provider, user_local, crypted_password, salt, " +
      "created_at, updated_at, remember_token, remember_token_expires_at) " +
      "values ('" + ADMIN_USER + "', 'Administrator', '', 'admin', 'sonarqube', ?, " +
      "'a373a0e667abb2604c1fd571eb4ad47fe8cc0878', '48bc4b0d93179b5103fd3885ea9119498e9d161b', ?, ?, null, null)")
      .setBoolean(1, true)
      .setLong(2, now)
      .setLong(3, now)
      .execute()
      .commit();
  }

  private static void insertGroupMemberships(Context context) throws SQLException {
    truncateTable(context, "groups_users");

    context.prepareUpsert("insert into groups_users (user_id, group_id) values " +
      "((select id from users where login='" + ADMIN_USER + "'), (select id from groups where name=?))")
      .setString(1, ADMINS_GROUP).addBatch()
      .setString(1, USERS_GROUP).addBatch()
      .execute()
      .commit();
  }

  private static void truncateTable(Context context, String table) throws SQLException {
    context.prepareUpsert("truncate table " + table).execute().commit();
  }

}
