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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.platform.db.migration.version.v103.CreateGithubPermissionsMappingTable.GITHUB_PERMISSIONS_MAPPING_TABLE_NAME;

public class PopulateGithubPermissionsMappingIT {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateGithubPermissionsMapping.class);
  @Rule
  public LogTester logTester = new LogTester();

  private final PopulateGithubPermissionsMapping migration = new PopulateGithubPermissionsMapping(db.database(), UuidFactoryFast.getInstance());

  @Test
  public void execute_whenTableAlreadyPopulated_doesNothing() throws SQLException {
    db.executeInsert(GITHUB_PERMISSIONS_MAPPING_TABLE_NAME,
      "UUID", UuidFactoryFast.getInstance().create(),
      "github_role", "gh_role",
      "sonarqube_permission", "sq_perm");

    migration.execute();

    assertThat(db.select("select github_role, sonarqube_permission from github_perms_mapping"))
      .extracting(stringObjectMap -> stringObjectMap.get("GITHUB_ROLE"), stringObjectMap -> stringObjectMap.get("SONARQUBE_PERMISSION"))
      .containsExactly(tuple("gh_role", "sq_perm"));
  }

  @Test
  public void execute_whenTableIsEmpty_shouldPopulate() throws SQLException {
    migration.execute();

    verifyMapping();
  }

  @Test
  public void execute_isReentrant() throws SQLException {
    migration.execute();
    migration.execute();
    migration.execute();

    verifyMapping();
  }

  private void verifyMapping() {
    assertThat(db.select("select github_role, sonarqube_permission from github_perms_mapping"))
      .extracting(stringObjectMap -> stringObjectMap.get("GITHUB_ROLE"), stringObjectMap -> stringObjectMap.get("SONARQUBE_PERMISSION"))
      .containsExactlyInAnyOrder(
        tuple("read", "codeviewer"),
        tuple("read", "user"),
        tuple("triage", "codeviewer"),
        tuple("triage", "user"),
        tuple("write", "codeviewer"),
        tuple("write", "user"),
        tuple("write", "issueadmin"),
        tuple("write", "securityhotspotadmin"),
        tuple("write", "scan"),
        tuple("maintain", "codeviewer"),
        tuple("maintain", "user"),
        tuple("maintain", "issueadmin"),
        tuple("maintain", "securityhotspotadmin"),
        tuple("maintain", "scan"),
        tuple("admin", "codeviewer"),
        tuple("admin", "user"),
        tuple("admin", "issueadmin"),
        tuple("admin", "securityhotspotadmin"),
        tuple("admin", "scan"),
        tuple("admin", "admin")
      );
  }

}
