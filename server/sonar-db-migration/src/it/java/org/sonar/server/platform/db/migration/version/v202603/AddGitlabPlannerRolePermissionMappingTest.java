/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202603;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

class AddGitlabPlannerRolePermissionMappingTest {

  private static final long NOW = 1_704_067_200_000L;

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddGitlabPlannerRolePermissionMapping.class);

  private final DataChange underTest = new AddGitlabPlannerRolePermissionMapping(db.database());

  @Test
  void execute_shouldInsertPlannerPermissions() throws SQLException {
    underTest.execute();

    List<Map<String, Object>> rows = db.select(
      "select sonarqube_permission from devops_perms_mapping where devops_platform = 'gitlab' and devops_platform_role = 'planner'");
    assertThat(rows)
      .extracting(r -> r.get("sonarqube_permission"))
      .containsExactlyInAnyOrder("user", "codeviewer");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    List<Map<String, Object>> rows = db.select(
      "select sonarqube_permission from devops_perms_mapping where devops_platform = 'gitlab' and devops_platform_role = 'planner'");
    assertThat(rows).hasSize(2);
  }

  @Test
  void execute_whenPlannerAlreadyExists_shouldNotInsert() throws SQLException {
    db.executeInsert("devops_perms_mapping",
      "uuid", "existing-uuid",
      "devops_platform", "gitlab",
      "devops_platform_role", "planner",
      "sonarqube_permission", "user");

    underTest.execute();

    List<Map<String, Object>> rows = db.select(
      "select sonarqube_permission from devops_perms_mapping where devops_platform = 'gitlab' and devops_platform_role = 'planner'");
    assertThat(rows).hasSize(1);
  }

  @Test
  void execute_whenGitlabProvisioningEnabled_shouldNotInsertPermissions() throws SQLException {
    db.executeInsert("properties",
      "uuid", "prop-uuid",
      "prop_key", "provisioning.gitlab.enabled",
      "is_empty", false,
      "text_value", "true",
      "created_at", NOW);

    underTest.execute();

    List<Map<String, Object>> rows = db.select(
      "select sonarqube_permission from devops_perms_mapping where devops_platform = 'gitlab' and devops_platform_role = 'planner'");
    assertThat(rows).isEmpty();
  }
}
