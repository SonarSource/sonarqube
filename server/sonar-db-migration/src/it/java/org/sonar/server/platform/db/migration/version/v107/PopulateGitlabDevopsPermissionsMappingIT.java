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
package org.sonar.server.platform.db.migration.version.v107;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.DEVOPS_PERMS_MAPPING_TABLE_NAME;

class PopulateGitlabDevopsPermissionsMappingIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateGitlabDevOpsPermissionsMapping.class);
  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final PopulateGitlabDevOpsPermissionsMapping migration = new PopulateGitlabDevOpsPermissionsMapping(db.database(), UuidFactoryImpl.INSTANCE);

  @Test
  void execute_whenTableIsEmpty_shouldPopulate() throws SQLException {
    migration.execute();

    verifyMapping();
  }

  @Test
  void execute_isReentrant() throws SQLException {
    migration.execute();
    migration.execute();

    verifyMapping();
  }

  @Test
  void execute_whenTableAlreadyPopulated_doesNothing() throws SQLException {
    db.executeInsert(DEVOPS_PERMS_MAPPING_TABLE_NAME,
      "UUID", UuidFactoryImpl.INSTANCE.create(),
      "devops_platform", "gitlab",
      "devops_platform_role", "role1",
      "sonarqube_permission", "sq_perm");

    migration.execute();

    assertThat(db.select("select devops_platform_role, sonarqube_permission from devops_perms_mapping"))
      .extracting(stringObjectMap -> stringObjectMap.get("DEVOPS_PLATFORM_ROLE"), stringObjectMap -> stringObjectMap.get("SONARQUBE_PERMISSION"))
      .containsExactly(tuple("role1", "sq_perm"));
  }

  @Test
  void execute_whenTableAlreadyPopulatedWithGithub_appliesMigration() throws SQLException {
    db.executeInsert(DEVOPS_PERMS_MAPPING_TABLE_NAME,
      "UUID", UuidFactoryImpl.INSTANCE.create(),
      "devops_platform", "github",
      "devops_platform_role", "role1",
      "sonarqube_permission", "sq_perm");

    migration.execute();

    verifyMapping();
  }

  private void verifyMapping() {
    assertThat(db.select("select devops_platform_role, sonarqube_permission from devops_perms_mapping where devops_platform = 'gitlab'"))
      .extracting(stringObjectMap -> stringObjectMap.get("DEVOPS_PLATFORM_ROLE"), stringObjectMap -> stringObjectMap.get("SONARQUBE_PERMISSION"))
      .containsExactlyInAnyOrder(
        tuple("guest", "user"),
        tuple("reporter", "codeviewer"),
        tuple("reporter", "user"),
        tuple("developer", "codeviewer"),
        tuple("developer", "user"),
        tuple("developer", "issueadmin"),
        tuple("developer", "securityhotspotadmin"),
        tuple("developer", "scan"),
        tuple("maintainer", "codeviewer"),
        tuple("maintainer", "user"),
        tuple("maintainer", "issueadmin"),
        tuple("maintainer", "securityhotspotadmin"),
        tuple("maintainer", "scan"),
        tuple("maintainer", "admin"),
        tuple("owner", "codeviewer"),
        tuple("owner", "user"),
        tuple("owner", "issueadmin"),
        tuple("owner", "securityhotspotadmin"),
        tuple("owner", "scan"),
        tuple("owner", "admin")
      );
  }

}
