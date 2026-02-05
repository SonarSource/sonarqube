/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.SCAN;
import static org.sonar.api.web.UserRole.SECURITYHOTSPOT_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.config.AiCodeFixEnablementConstants.SUGGESTION_FEATURE_ENABLED_PROPERTY;
import static org.sonar.core.config.AiCodeFixEnablementConstants.SUGGESTION_PROVIDER_KEY_PROPERTY;
import static org.sonar.core.config.AiCodeFixEnablementConstants.SUGGESTION_PROVIDER_MODEL_KEY_PROPERTY;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

public class PopulateInitialSchema extends DataChange {

  private static final String ADMINS_GROUP = "sonar-administrators";
  private static final String USERS_GROUP = "sonar-users";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_CRYPTED_PASSWORD = "100000$R9xDN18ebKxA3ZTaputi6wDt+fcKhP2h3GgAjGbcBlCSlkMLENxw9wziHS46QIW3fWOjEMpeyEts+pNuPXSbYA==";
  private static final String ADMIN_SALT = "pSDhsn3IM3KCa74CRRf7T7Vx+OE=";
  private static final List<String> ADMIN_ROLES = Arrays.asList("admin", "profileadmin", "gateadmin", "provisioning", "applicationcreator", "portfoliocreator");
  private static final String DISABLED = "DISABLED";
  private static final String DEFAULT_AI_PROVIDER_KEY = "OPENAI";
  private static final String DEFAULT_AI_PROVIDER_MODEL_KEY = "OPENAI_GPT_4O";

  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final SonarQubeVersion sonarQubeVersion;
  private final MigrationHistory migrationHistory;

  public PopulateInitialSchema(Database db, System2 system2, UuidFactory uuidFactory, SonarQubeVersion sonarQubeVersion,
    MigrationHistory migrationHistory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.sonarQubeVersion = sonarQubeVersion;
    this.migrationHistory = migrationHistory;
  }

  @Override
  public void execute(Context context) throws SQLException {
    String adminUserUuid = insertAdminUser(context);
    Groups groups = insertGroups(context);
    String defaultQGUuid = insertQualityGate(context);
    insertInternalProperty(context);
    insertProperties(context, defaultQGUuid);
    insertGroupRoles(context, groups);
    insertGroupUsers(context, adminUserUuid, groups);
    insertDevopsPermissionMapping(context);
    insertGitlabPermissionMapping(context);
    insertDefaultAiSuggestionProperties(context);
    enableSpecificMqrMode(context);
  }

  private void insertGitlabPermissionMapping(Context context) throws SQLException {
    String insertQuery = """
    insert into devops_perms_mapping (uuid, devops_platform, devops_platform_role, sonarqube_permission)
    values (?, ?, ?, ?)
    """;

    Map<String, Set<String>> gitlabRoleToSqPermissions = Map.of(
      "guest", Set.of(USER),
      "reporter", Set.of(USER, CODEVIEWER),
      "developer", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN),
      "maintainer", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN, ADMIN),
      "owner", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN, ADMIN)
    );

    try (Upsert upsert = context.prepareUpsert(insertQuery)) {
      gitlabRoleToSqPermissions.forEach((role, permissions) -> insertGitlabRoleToSonarqubePermissionMapping(upsert, role, permissions));
      upsert.commit();
    }
  }

  private void insertGitlabRoleToSonarqubePermissionMapping(Upsert upsert, String role, Set<String> sonarqubePermissions) {
    sonarqubePermissions.forEach(permission -> insertGitlabRoleToSonarqubePermissionMapping(upsert, role, permission));
  }

  private void insertGitlabRoleToSonarqubePermissionMapping(Upsert upsert, String role, String sonarqubePermission) {
    try {
      upsert
        .setString(1, uuidFactory.create())
        .setString(2, "gitlab")
        .setString(3, role)
        .setString(4, sonarqubePermission)
        .execute();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private String insertAdminUser(Context context) throws SQLException {
    truncateTable(context, "users");

    long now = system2.now();
    context.prepareUpsert("insert into users " +
      "(uuid, login, name, email, external_id, external_login, external_identity_provider, user_local, crypted_password, salt, hash_method, reset_password, " +
      "created_at, updated_at)" +
      " values " +
      "(?, ?, 'Administrator', null, 'admin', 'admin', 'sonarqube', ?, ?, ?, 'PBKDF2', ?, ?, ?)")
      .setString(1, uuidFactory.create())
      .setString(2, ADMIN_USER)
      .setBoolean(3, true)
      .setString(4, ADMIN_CRYPTED_PASSWORD)
      .setString(5, ADMIN_SALT)
      .setBoolean(6, true)
      .setLong(7, now)
      .setLong(8, now)
      .execute()
      .commit();

    String res = context.prepareSelect("select uuid from users where login=?")
      .setString(1, ADMIN_USER)
      .get(t -> t.getString(1));
    return requireNonNull(res);
  }

  private void insertInternalProperty(Context context) throws SQLException {
    String tableName = "internal_properties";
    truncateTable(context, tableName);

    long now = system2.now();
    Upsert upsert = context.prepareUpsert(createInsertStatement(tableName, "kee", "is_empty", "text_value", "created_at"));
    upsert
      .setString(1, "installation.date")
      .setBoolean(2, false)
      .setString(3, String.valueOf(system2.now()))
      .setLong(4, now)
      .addBatch();
    upsert
      .setString(1, "installation.version")
      .setBoolean(2, false)
      .setString(3, sonarQubeVersion.get().toString())
      .setLong(4, now)
      .addBatch();
    upsert
      .execute()
      .commit();
  }

  private void insertProperties(Context context, String defaultQualityGate) throws SQLException {
    var tableName = "properties";
    truncateTable(context, tableName);

    long now = system2.now();
    var upsert = context
      .prepareUpsert(createInsertStatement(tableName, "uuid", "prop_key", "is_empty", "text_value", "created_at"));

    upsert.setString(1, uuidFactory.create())
      .setString(2, "sonar.forceAuthentication")
      .setBoolean(3, false)
      .setString(4, "true")
      .setLong(5, now)
      .addBatch();

    upsert.setString(1, uuidFactory.create())
      .setString(2, CorePropertyDefinitions.ALLOW_DISABLE_INHERITED_RULES)
      .setBoolean(3, false)
      .setString(4, "true")
      .setLong(5, now)
      .addBatch();

    upsert
      .setString(1, uuidFactory.create())
      .setString(2, "projects.default.visibility")
      .setBoolean(3, false)
      .setString(4, "public")
      .setLong(5, now)
      .addBatch();

    upsert
      .setString(1, uuidFactory.create())
      .setString(2, "qualitygate.default")
      .setBoolean(3, false)
      .setString(4, defaultQualityGate)
      .setLong(5, system2.now())
      .addBatch();

    upsert
      .execute()
      .commit();
  }

  private Groups insertGroups(Context context) throws SQLException {
    truncateTable(context, "groups");

    Date now = new Date(system2.now());
    Upsert upsert = context.prepareUpsert(createInsertStatement(
      "groups",
      "uuid", "name", "description", "created_at", "updated_at"));
    upsert
      .setString(1, uuidFactory.create())
      .setString(2, ADMINS_GROUP)
      .setString(3, "System administrators")
      .setDate(4, now)
      .setDate(5, now)
      .addBatch();
    upsert
      .setString(1, uuidFactory.create())
      .setString(2, USERS_GROUP)
      .setString(3, "Every authenticated user automatically belongs to this group")
      .setDate(4, now)
      .setDate(5, now)
      .addBatch();
    upsert
      .execute()
      .commit();

    return new Groups(getGroupUuid(context, ADMINS_GROUP), getGroupUuid(context, USERS_GROUP));
  }

  private static String getGroupUuid(Context context, String groupName) throws SQLException {
    String res = context.prepareSelect("select uuid from groups where name=?")
      .setString(1, groupName)
      .get(t -> t.getString(1));
    return requireNonNull(res);
  }

  private String insertQualityGate(Context context) throws SQLException {
    truncateTable(context, "quality_gates");

    String uuid = uuidFactory.create();
    Date now = new Date(system2.now());
    context.prepareUpsert(createInsertStatement("quality_gates", "uuid", "name", "is_built_in", "created_at", "updated_at"))
      .setString(1, uuid)
      .setString(2, "Sonar way")
      .setBoolean(3, true)
      .setDate(4, now)
      .setDate(5, now)
      .execute()
      .commit();
    return uuid;
  }

  private record Groups(String adminGroupUuid, String userGroupUuid) {
  }

  private void insertGroupRoles(Context context, Groups groups) throws SQLException {
    truncateTable(context, "group_roles");

    Upsert upsert = context.prepareUpsert(createInsertStatement("group_roles", "uuid", "group_uuid", "role"));
    for (String adminRole : ADMIN_ROLES) {
      upsert
        .setString(1, uuidFactory.create())
        .setString(2, groups.adminGroupUuid())
        .setString(3, adminRole)
        .addBatch();
    }
    for (String anyoneRole : Arrays.asList("scan", "provisioning")) {
      upsert
        .setString(1, uuidFactory.create())
        .setString(2, groups.userGroupUuid())
        .setString(3, anyoneRole)
        .addBatch();
    }
    upsert
      .execute()
      .commit();
  }

  private void insertDevopsPermissionMapping(Context context) throws SQLException {
    Map<String, Set<String>> devopsRoleToSqPermissions = Map.of(
      "read", Set.of(USER, CODEVIEWER),
      "triage", Set.of(USER, CODEVIEWER),
      "write", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN),
      "maintain", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN),
      "admin", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN, ADMIN));

    String insertQuery = """
    insert into devops_perms_mapping (uuid, devops_platform_role, sonarqube_permission)
    values (?, ?, ?)
    """;
    try (Upsert upsert = context.prepareUpsert(insertQuery)) {
      devopsRoleToSqPermissions.forEach((key, value) -> insertGithubRoleToSonarqubePermissionMapping(upsert, key, value));
      upsert.commit();
    }
  }

  private void insertGithubRoleToSonarqubePermissionMapping(Upsert upsert, String githubRole, Set<String> sonarqubePermissions) {
    sonarqubePermissions.forEach(permission -> insertGithubRoleToSonarqubePermissionMapping(upsert, githubRole, permission));
  }

  private void insertGithubRoleToSonarqubePermissionMapping(Upsert upsert, String githubRole, String sonarqubePermission) {
    try {
      upsert
        .setString(1, uuidFactory.create())
        .setString(2, githubRole)
        .setString(3, sonarqubePermission)
        .execute();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private void insertDefaultAiSuggestionProperties(Context context) throws SQLException {
    boolean isAiCodeFixEnabled = Optional.ofNullable(context.prepareSelect("select text_value from properties where prop_key=?")
        .setString(1, SUGGESTION_FEATURE_ENABLED_PROPERTY)
        .get(r -> r.getString(1)))
      .map(value -> !DISABLED.equals(value))
      .orElse(false);

    if (isAiCodeFixEnabled) {
      insertProperty(context, SUGGESTION_PROVIDER_KEY_PROPERTY, DEFAULT_AI_PROVIDER_KEY);
      insertProperty(context, SUGGESTION_PROVIDER_MODEL_KEY_PROPERTY, DEFAULT_AI_PROVIDER_MODEL_KEY);
    }
  }

  private void insertProperty(Context context, String key, String value) throws SQLException {
    context.prepareUpsert("""
        INSERT INTO properties
        (prop_key, is_empty, text_value, created_at, uuid)
        VALUES(?, ?, ?, ?, ?)
      """)
      .setString(1, key)
      .setBoolean(2, false)
      .setString(3, value)
      .setLong(4, system2.now())
      .setString(5, uuidFactory.create())
      .execute()
      .commit();
  }

  private void enableSpecificMqrMode(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!paramExists(connection)) {
        long version = migrationHistory.getInitialDbVersion();
        boolean mqrModeEnabled = version >= 102_000L || version == -1L;
        Upsert upsert = context.prepareUpsert(
          createInsertStatement("properties",
            "uuid",
            "prop_key",
            "is_empty",
            "text_value",
            "created_at"));
        upsert.setString(1, uuidFactory.create())
          .setString(2, MULTI_QUALITY_MODE_ENABLED)
          .setBoolean(3, false)
          .setString(4, String.valueOf(mqrModeEnabled))
          .setLong(5, system2.now());
        upsert.execute().commit();
      }
    }
  }

  private static boolean paramExists(Connection connection) throws SQLException {
    String sql = "SELECT count(1) FROM properties WHERE prop_key = '" + MULTI_QUALITY_MODE_ENABLED + "'";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      ResultSet result = statement.executeQuery();
      return result.next() && result.getInt(1) > 0;
    }
  }

  private void insertGroupUsers(Context context, String adminUserUuid, Groups groups) throws SQLException {
    truncateTable(context, "groups_users");

    Upsert upsert = context.prepareUpsert(createInsertStatement("groups_users", "uuid", "user_uuid", "group_uuid"));
    upsert
      .setString(1, uuidFactory.create())
      .setString(2, adminUserUuid)
      .setString(3, groups.userGroupUuid())
      .addBatch();
    upsert
      .setString(1, uuidFactory.create())
      .setString(2, adminUserUuid)
      .setString(3, groups.adminGroupUuid())
      .addBatch();
    upsert
      .execute()
      .commit();
  }

  private static void truncateTable(Context context, String table) throws SQLException {
    context.prepareUpsert("truncate table " + table).execute().commit();
  }

  public static String createInsertStatement(String tableName, String firstColumn, String... otherColumns) {
    return "insert into " + tableName + " " +
      "(" + concat(of(firstColumn), stream(otherColumns)).collect(joining(",")) + ")" +
      " values" +
      " (" + concat(of(firstColumn), stream(otherColumns)).map(t -> "?").collect(joining(",")) + ")";
  }

}
