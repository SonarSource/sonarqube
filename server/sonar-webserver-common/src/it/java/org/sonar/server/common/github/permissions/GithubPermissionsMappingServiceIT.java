/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.common.github.permissions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.provisioning.GithubPermissionsMappingDao;
import org.sonar.db.provisioning.GithubPermissionsMappingDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.ADMIN_GITHUB_ROLE;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.MAINTAIN_GITHUB_ROLE;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.READ_GITHUB_ROLE;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.TRIAGE_GITHUB_ROLE;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.WRITE_GITHUB_ROLE;

public class GithubPermissionsMappingServiceIT {

  @Rule
  public DbTester db = DbTester.create();
  private final DbSession dbSession = db.getSession();

  private final AuditPersister auditPersister = mock();
  private final GithubPermissionsMappingDao githubPermissionsMappingDao = new GithubPermissionsMappingDao(auditPersister);

  private final GithubPermissionsMappingService underTest = new GithubPermissionsMappingService(db.getDbClient(), githubPermissionsMappingDao);

  @Test
  public void getPermissionsMapping_whenMappingNotDefined_returnMappingEntirelyFalse() {
    List<GithubPermissionsMapping> actualPermissionsMapping = underTest.getPermissionsMapping();

    List<GithubPermissionsMapping> expectedPermissionsMapping = List.of(
      new GithubPermissionsMapping(READ_GITHUB_ROLE, new SonarqubePermissions(false, false, false, false, false, false)),
      new GithubPermissionsMapping(TRIAGE_GITHUB_ROLE, new SonarqubePermissions(false, false, false, false, false, false)),
      new GithubPermissionsMapping(WRITE_GITHUB_ROLE, new SonarqubePermissions(false, false, false, false, false, false)),
      new GithubPermissionsMapping(MAINTAIN_GITHUB_ROLE, new SonarqubePermissions(false, false, false, false, false, false)),
      new GithubPermissionsMapping(ADMIN_GITHUB_ROLE, new SonarqubePermissions(false, false, false, false, false, false))
    );

    assertThat(actualPermissionsMapping).containsAll(expectedPermissionsMapping);
  }

  @Test
  public void getPermissionsMapping_whenMappingDefined_returnMapping() {
    Map<String, Set<String>> githubRolesToSqPermissions = Map.of(
      READ_GITHUB_ROLE, Set.of("user", "codeviewer"),
      WRITE_GITHUB_ROLE, Set.of("user", "codeviewer", "issueadmin", "securityhotspotadmin", "admin", "scan")
    );
    persistGithubPermissionsMapping(githubRolesToSqPermissions);

    List<GithubPermissionsMapping> actualPermissionsMapping = underTest.getPermissionsMapping();

    List<GithubPermissionsMapping> expectedPermissionsMapping = List.of(
      new GithubPermissionsMapping(READ_GITHUB_ROLE, new SonarqubePermissions(true, true, false, false, false, false)),
      new GithubPermissionsMapping(TRIAGE_GITHUB_ROLE, new SonarqubePermissions(false, false, false, false, false, false)),
      new GithubPermissionsMapping(WRITE_GITHUB_ROLE, new SonarqubePermissions(true, true, true, true, true, true)),
      new GithubPermissionsMapping(MAINTAIN_GITHUB_ROLE, new SonarqubePermissions(false, false, false, false, false, false)),
      new GithubPermissionsMapping(ADMIN_GITHUB_ROLE, new SonarqubePermissions(false, false, false, false, false, false))
    );

    assertThat(actualPermissionsMapping).containsAll(expectedPermissionsMapping);
  }

  private void persistGithubPermissionsMapping(Map<String, Set<String>> githubRolesToSonarqubePermissions) {
    for (Map.Entry<String, Set<String>> githubRoleToSonarqubePermissions : githubRolesToSonarqubePermissions.entrySet()) {
      String githubRole = githubRoleToSonarqubePermissions.getKey();
      githubRoleToSonarqubePermissions.getValue()
        .forEach(permission -> githubPermissionsMappingDao.insert(
          dbSession,
          new GithubPermissionsMappingDto("uuid_" + githubRole + "_" + permission, githubRole, permission)));
    }
    dbSession.commit();
  }

}
