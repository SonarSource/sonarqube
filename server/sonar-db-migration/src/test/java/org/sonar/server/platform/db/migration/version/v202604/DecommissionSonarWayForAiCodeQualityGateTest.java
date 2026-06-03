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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class DecommissionSonarWayForAiCodeQualityGateTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DecommissionSonarWayForAiCodeQualityGate.class);

  private final DecommissionSonarWayForAiCodeQualityGate underTest = new DecommissionSonarWayForAiCodeQualityGate(db.database());

  @Test
  void execute_whenGateDoesNotExist_shouldDoNothing() throws SQLException {
    underTest.execute();

    assertThat(db.select("SELECT uuid FROM quality_gates")).isEmpty();
  }

  @Test
  void execute_whenGateIsUsedAsInstanceDefault_shouldRenameToLegacy() throws SQLException {
    String gateUuid = "gate-uuid-1";
    insertQualityGate(gateUuid, DecommissionSonarWayForAiCodeQualityGate.GATE_NAME, true);
    insertCondition("cond-uuid-1", gateUuid);
    insertInstanceDefaultProperty(gateUuid);

    underTest.execute();

    List<Map<String, Object>> gates = db.select("SELECT name, is_built_in, ai_code_supported FROM quality_gates WHERE uuid = '" + gateUuid + "'");
    assertThat(gates).hasSize(1);
    assertThat(gates.getFirst()).containsEntry("NAME", DecommissionSonarWayForAiCodeQualityGate.LEGACY_GATE_NAME);
    assertThat(gates.getFirst()).containsEntry("IS_BUILT_IN", false);
    assertThat(gates.getFirst()).containsEntry("AI_CODE_SUPPORTED", true);

    assertThat(db.select("SELECT uuid FROM quality_gate_conditions WHERE qgate_uuid = '" + gateUuid + "'")).hasSize(1);
  }

  @Test
  void execute_whenGateIsAssignedToProject_shouldRenameToLegacy() throws SQLException {
    String gateUuid = "gate-uuid-2";
    insertQualityGate(gateUuid, DecommissionSonarWayForAiCodeQualityGate.GATE_NAME, true);
    insertCondition("cond-uuid-2", gateUuid);
    insertProjectQGate("project-uuid-1", gateUuid);

    underTest.execute();

    List<Map<String, Object>> gates = db.select("SELECT name, is_built_in, ai_code_supported FROM quality_gates WHERE uuid = '" + gateUuid + "'");
    assertThat(gates).hasSize(1);
    assertThat(gates.getFirst()).containsEntry("NAME", DecommissionSonarWayForAiCodeQualityGate.LEGACY_GATE_NAME);
    assertThat(gates.getFirst()).containsEntry("IS_BUILT_IN", false);
    assertThat(gates.getFirst()).containsEntry("AI_CODE_SUPPORTED", true);

    assertThat(db.select("SELECT uuid FROM quality_gate_conditions WHERE qgate_uuid = '" + gateUuid + "'")).hasSize(1);
  }

  @Test
  void execute_whenGateIsUnused_shouldDeleteGateConditionsAndPermissions() throws SQLException {
    String gateUuid = "gate-uuid-3";
    insertQualityGate(gateUuid, DecommissionSonarWayForAiCodeQualityGate.GATE_NAME, true);
    insertCondition("cond-uuid-3a", gateUuid);
    insertCondition("cond-uuid-3b", gateUuid);
    insertUserPermission("user-perm-uuid-3", gateUuid);
    insertGroupPermission("group-perm-uuid-3", gateUuid);

    underTest.execute();

    assertThat(db.select("SELECT uuid FROM quality_gates WHERE uuid = '" + gateUuid + "'")).isEmpty();
    assertThat(db.select("SELECT uuid FROM quality_gate_conditions WHERE qgate_uuid = '" + gateUuid + "'")).isEmpty();
    assertThat(db.select("SELECT uuid FROM qgate_user_permissions WHERE quality_gate_uuid = '" + gateUuid + "'")).isEmpty();
    assertThat(db.select("SELECT uuid FROM qgate_group_permissions WHERE quality_gate_uuid = '" + gateUuid + "'")).isEmpty();
  }

  @Test
  void execute_whenGateIsUnused_isIdempotent() throws SQLException {
    String gateUuid = "gate-uuid-4";
    insertQualityGate(gateUuid, DecommissionSonarWayForAiCodeQualityGate.GATE_NAME, true);

    underTest.execute();
    underTest.execute();

    assertThat(db.select("SELECT uuid FROM quality_gates WHERE uuid = '" + gateUuid + "'")).isEmpty();
  }

  @Test
  void execute_whenGateIsAssignedToProject_isIdempotent() throws SQLException {
    String gateUuid = "gate-uuid-5";
    insertQualityGate(gateUuid, DecommissionSonarWayForAiCodeQualityGate.GATE_NAME, true);
    insertProjectQGate("project-uuid-2", gateUuid);

    underTest.execute();
    underTest.execute();

    List<Map<String, Object>> gates = db.select("SELECT name, is_built_in FROM quality_gates WHERE uuid = '" + gateUuid + "'");
    assertThat(gates).hasSize(1);
    assertThat(gates.getFirst()).containsEntry("NAME", DecommissionSonarWayForAiCodeQualityGate.LEGACY_GATE_NAME);
    assertThat(gates.getFirst()).containsEntry("IS_BUILT_IN", false);
  }

  private void insertQualityGate(String uuid, String name, boolean isBuiltIn) {
    db.executeInsert("quality_gates",
      "uuid", uuid,
      "name", name,
      "is_built_in", isBuiltIn,
      "ai_code_supported", true);
  }

  private void insertCondition(String uuid, String gateUuid) {
    db.executeInsert("quality_gate_conditions",
      "uuid", uuid,
      "qgate_uuid", gateUuid,
      "metric_uuid", "metric-uuid-1");
  }

  private void insertInstanceDefaultProperty(String gateUuid) {
    db.executeInsert("properties",
      "uuid", "prop-uuid-1",
      "prop_key", "qualitygate.default",
      "is_empty", false,
      "text_value", gateUuid,
      "created_at", 1_000_000L);
  }

  private void insertProjectQGate(String projectUuid, String gateUuid) {
    db.executeInsert("project_qgates",
      "project_uuid", projectUuid,
      "quality_gate_uuid", gateUuid);
  }

  private void insertUserPermission(String uuid, String gateUuid) {
    db.executeInsert("qgate_user_permissions",
      "uuid", uuid,
      "quality_gate_uuid", gateUuid,
      "user_uuid", "user-uuid-1",
      "created_at", 1_000_000L);
  }

  private void insertGroupPermission(String uuid, String gateUuid) {
    db.executeInsert("qgate_group_permissions",
      "uuid", uuid,
      "quality_gate_uuid", gateUuid,
      "group_uuid", "group-uuid-1",
      "created_at", 1_000_000L);
  }
}
