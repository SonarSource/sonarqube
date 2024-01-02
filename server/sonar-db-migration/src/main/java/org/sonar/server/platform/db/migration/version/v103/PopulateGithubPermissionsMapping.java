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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.Upsert;

import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.SCAN;
import static org.sonar.api.web.UserRole.SECURITYHOTSPOT_ADMIN;
import static org.sonar.api.web.UserRole.USER;

public class PopulateGithubPermissionsMapping extends DataChange {

  private static final Map<String, Set<String>> GITHUB_ROLE_TO_SQ_PERMISSIONS = Map.of(
    "read", Set.of(USER, CODEVIEWER),
    "triage", Set.of(USER, CODEVIEWER),
    "write", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN),
    "maintain", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN),
    "admin", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN, ADMIN)
  );

  private static final String INSERT_QUERY = """
    insert into github_perms_mapping (uuid, github_role, sonarqube_permission)
    values (?, ?, ?)
    """;

  private final UuidFactory uuidFactory;

  public PopulateGithubPermissionsMapping(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (isDefaultMappingAlreadyDefined(context)) {
      return;
    }
    try (Upsert upsert = context.prepareUpsert(INSERT_QUERY)) {
      GITHUB_ROLE_TO_SQ_PERMISSIONS.forEach((key, value) -> insertGithubRoleToSonarqubePermissionMapping(upsert, key, value));
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

  private static boolean isDefaultMappingAlreadyDefined(Context context) throws SQLException {
    try (Select select = context.prepareSelect("select count(*) from github_perms_mapping")) {
      return Optional.ofNullable(select.get(t -> t.getInt(1) > 0))
        .orElseThrow();
    }
  }

}
