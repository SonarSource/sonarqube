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
package org.sonar.server.platform.db.migration.version.v107;

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

public class PopulateGitlabDevOpsPermissionsMapping extends DataChange {

  private static final Map<String, Set<String>> GITLAB_ROLE_TO_SQ_PERMISSIONS = Map.of(
    "guest", Set.of(USER),
    "reporter", Set.of(USER, CODEVIEWER),
    "developer", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN),
    "maintainer", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN, ADMIN),
    "owner", Set.of(USER, CODEVIEWER, ISSUE_ADMIN, SECURITYHOTSPOT_ADMIN, SCAN, ADMIN)
  );

  private static final String INSERT_QUERY = """
    insert into devops_perms_mapping (uuid, devops_platform, devops_platform_role, sonarqube_permission)
    values (?, ?, ?, ?)
    """;

  private final UuidFactory uuidFactory;

  public PopulateGitlabDevOpsPermissionsMapping(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (isDefaultMappingAlreadyDefined(context)) {
      return;
    }
    try (Upsert upsert = context.prepareUpsert(INSERT_QUERY)) {
      GITLAB_ROLE_TO_SQ_PERMISSIONS.forEach((role, permissions) -> insertGitlabRoleToSonarqubePermissionMapping(upsert, role, permissions));
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

  private static boolean isDefaultMappingAlreadyDefined(Context context) throws SQLException {
    try (Select select = context.prepareSelect("select count(*) from devops_perms_mapping where devops_platform='gitlab'")) {
      return Optional.ofNullable(select.get(t -> t.getInt(1) > 0))
        .orElseThrow();
    }
  }

}
