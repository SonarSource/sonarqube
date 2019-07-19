/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v00;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class PopulateInitialSchema extends DataChange {

  private static final String ADMINS_GROUP = "sonar-administrators";
  private static final String USERS_GROUP = "sonar-users";
  private static final String ADMIN_USER = "admin";
  private static final List<String> ADMIN_ROLES = Arrays.asList("admin", "profileadmin", "gateadmin", "provisioning", "applicationcreator", "portfoliocreator");

  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final SonarRuntime sonarRuntime;

  public PopulateInitialSchema(Database db, System2 system2, UuidFactory uuidFactory, SonarRuntime sonarRuntime) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.sonarRuntime = sonarRuntime;
  }

  @Override
  public void execute(Context context) throws SQLException {
    String organizationUuid = uuidFactory.create();

    int adminUserId = insertAdminUser(context);
    Groups groups = insertGroups(context, organizationUuid);
    String defaultQGUuid = insertQualityGate(context);
    insertOrganization(context, organizationUuid, groups, defaultQGUuid);
    insertOrgQualityGate(context, organizationUuid, defaultQGUuid);
    insertInternalProperty(context, organizationUuid);
    insertGroupRoles(context, organizationUuid, groups);
    insertGroupUsers(context, adminUserId, groups);
    insertOrganizationMember(context, adminUserId, organizationUuid);
  }

  private int insertAdminUser(Context context) throws SQLException {
    truncateTable(context, "users");

    long now = system2.now();
    context.prepareUpsert("insert into users " +
      "(uuid, login, name, email, external_id, external_login, external_identity_provider, user_local, crypted_password, salt, hash_method, is_root, onboarded, created_at, updated_at)"
      +
      " values " +
      "(?, ?, 'Administrator', null, 'admin', 'admin', 'sonarqube', ?, '$2a$12$uCkkXmhW5ThVK8mpBvnXOOJRLd64LJeHTeCkSuB3lfaR2N0AYBaSi', null, 'BCRYPT', ?, ?, ?, ?)")
      .setString(1, uuidFactory.create())
      .setString(2, ADMIN_USER)
      .setBoolean(3, true)
      .setBoolean(4, false)
      .setBoolean(5, true)
      .setLong(6, now)
      .setLong(7, now)
      .execute()
      .commit();

    return context.prepareSelect("select id from users where login=?")
      .setString(1, ADMIN_USER)
      .get(t -> t.getInt(1));
  }

  private void insertOrganization(Context context, String organizationUuid, Groups groups, String defaultQGUuid) throws SQLException {
    truncateTable(context, "organizations");

    long now = system2.now();
    context.prepareUpsert("insert into organizations " +
      "(uuid, kee, name, guarded, new_project_private, default_group_id, default_quality_gate_uuid, subscription, created_at, updated_at)" +
      " values " +
      "(?, 'default-organization', 'Default Organization', ?, ?, ?, ?, 'SONARQUBE', ?, ?)")
      .setString(1, organizationUuid)
      .setBoolean(2, true)
      .setBoolean(3, false)
      .setInt(4, groups.getUserGroupId())
      .setString(5, defaultQGUuid)
      .setLong(6, now)
      .setLong(7, now)
      .execute()
      .commit();
  }

  private void insertOrgQualityGate(Context context, String organizationUuid, String defaultQGUuid) throws SQLException {
    truncateTable(context, "org_quality_gates");

    context.prepareUpsert("insert into org_quality_gates " +
      "(uuid, organization_uuid, quality_gate_uuid)" +
      " values " +
      "(?, ?, ?)")
      .setString(1, uuidFactory.create())
      .setString(2, organizationUuid)
      .setString(3, defaultQGUuid)
      .execute()
      .commit();
  }

  private void insertInternalProperty(Context context, String organizationUuid) throws SQLException {
    truncateTable(context, "internal_properties");

    long now = system2.now();
    Upsert upsert = context.prepareUpsert("insert into internal_properties " +
      "(kee, is_empty, text_value, created_at)" +
      " values" +
      " (?, ?, ?, ?)");
    upsert
      .setString(1, "organization.default")
      .setBoolean(2, false)
      .setString(3, organizationUuid)
      .setLong(4, now)
      .addBatch();
    upsert
      .setString(1, "installation.date")
      .setBoolean(2, false)
      .setString(3, String.valueOf(system2.now()))
      .setLong(4, now)
      .addBatch();
    upsert
      .setString(1, "installation.version")
      .setBoolean(2, false)
      .setString(3, sonarRuntime.getApiVersion().toString())
      .setLong(4, now)
      .addBatch();
    upsert
      .execute()
      .commit();
  }

  private Groups insertGroups(Context context, String organizationUuid) throws SQLException {
    truncateTable(context, "groups");

    Date now = new Date(system2.now());
    Upsert upsert = context.prepareUpsert("insert into groups (organization_uuid, name, description, created_at, updated_at) values (?, ?, ?, ?, ?)");
    upsert
      .setString(1, organizationUuid)
      .setString(2, ADMINS_GROUP)
      .setString(3, "System administrators")
      .setDate(4, now)
      .setDate(5, now)
      .addBatch();
    upsert
      .setString(1, organizationUuid)
      .setString(2, USERS_GROUP)
      .setString(3, "Any new users created will automatically join this group")
      .setDate(4, now)
      .setDate(5, now)
      .addBatch();
    upsert
      .execute()
      .commit();

    return new Groups(getGroupId(context, ADMINS_GROUP), getGroupId(context, USERS_GROUP));
  }

  private static int getGroupId(Context context, String groupName) throws SQLException {
    return context.prepareSelect("select id from groups where name=?")
      .setString(1, groupName)
      .get(t -> t.getInt(1));
  }

  private String insertQualityGate(Context context) throws SQLException {
    truncateTable(context, "quality_gates");

    String uuid = uuidFactory.create();
    Date now = new Date(system2.now());
    context.prepareUpsert("insert into quality_gates " +
      "(uuid, name, is_built_in, created_at, updated_at)" +
      " values " +
      "(?, 'Sonar way', ?, ?, ?)")
      .setString(1, uuid)
      .setBoolean(2, true)
      .setDate(3, now)
      .setDate(4, now)
      .execute()
      .commit();
    return uuid;
  }

  private static final class Groups {
    private final int adminGroupId;
    private final int userGroupId;

    private Groups(int adminGroupId, int userGroupId) {
      this.adminGroupId = adminGroupId;
      this.userGroupId = userGroupId;
    }

    public int getAdminGroupId() {
      return adminGroupId;
    }

    public int getUserGroupId() {
      return userGroupId;
    }
  }

  private static void insertGroupRoles(Context context, String organizationUuid, Groups groups) throws SQLException {
    truncateTable(context, "group_roles");

    Upsert upsert = context.prepareUpsert("insert into group_roles (organization_uuid, group_id, role) values (?, ?, ?)");
    for (String adminRole : ADMIN_ROLES) {
      upsert
        .setString(1, organizationUuid)
        .setInt(2, groups.getAdminGroupId())
        .setString(3, adminRole)
        .addBatch();
    }
    for (String anyoneRole : Arrays.asList("scan", "provisioning")) {
      upsert
        .setString(1, organizationUuid)
        .setInt(2, null)
        .setString(3, anyoneRole)
        .addBatch();
    }
    upsert
      .execute()
      .commit();
  }

  private static void insertGroupUsers(Context context, int adminUserId, Groups groups) throws SQLException {
    truncateTable(context, "groups_users");

    Upsert upsert = context.prepareUpsert("insert into groups_users (user_id, group_id) values (?,?)");
    upsert
      .setInt(1, adminUserId)
      .setInt(2, groups.getUserGroupId())
      .addBatch();
    upsert
      .setInt(1, adminUserId)
      .setInt(2, groups.getAdminGroupId())
      .addBatch();
    upsert
      .execute()
      .commit();
  }

  private static void insertOrganizationMember(Context context, int adminUserId, String organizationUuid) throws SQLException {
    truncateTable(context, "organization_members");

    context.prepareUpsert("insert into organization_members(organization_uuid, user_id) values (?, ?)")
      .setString(1, organizationUuid)
      .setInt(2, adminUserId)
      .execute()
      .commit();
  }

  private static void truncateTable(Context context, String table) throws SQLException {
    context.prepareUpsert("truncate table " + table).execute().commit();
  }

}
