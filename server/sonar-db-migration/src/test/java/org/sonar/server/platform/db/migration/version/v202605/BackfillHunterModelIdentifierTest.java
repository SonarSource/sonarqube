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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class BackfillHunterModelIdentifierTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(BackfillHunterModelIdentifier.class);

  private final BackfillHunterModelIdentifier underTest = new BackfillHunterModelIdentifier(db.database());

  @Test
  void execute_shouldBackfillOnlyHunterMappingsWithoutModel() throws SQLException {
    insertMapping("hunter-without-model", BackfillHunterModelIdentifier.HUNTER_AGENT, null);
    insertMapping("remediation-without-model", "REMEDIATION_AGENT", null);

    underTest.execute();

    Map<String, Object> hunterMapping = db.select("select model_identifier from llm_provider_mappings where id = 'hunter-without-model'").getFirst();
    Map<String, Object> remediationMapping = db.select("select model_identifier from llm_provider_mappings where id = 'remediation-without-model'").getFirst();
    assertThat(hunterMapping).containsEntry("MODEL_IDENTIFIER", BackfillHunterModelIdentifier.HUNTER_MODEL_IDENTIFIER);
    assertThat(remediationMapping).containsEntry("MODEL_IDENTIFIER", null);
  }

  @Test
  void execute_shouldLeaveExistingHunterModelUnchanged() throws SQLException {
    insertMapping("hunter-with-model", BackfillHunterModelIdentifier.HUNTER_AGENT, "customer-model");

    underTest.execute();

    Map<String, Object> hunterMapping = db.select("select model_identifier from llm_provider_mappings where id = 'hunter-with-model'").getFirst();
    assertThat(hunterMapping).containsEntry("MODEL_IDENTIFIER", "customer-model");
  }

  @Test
  void execute_shouldBackfillBlankHunterModelAndBeReentrant() throws SQLException {
    insertMapping("hunter-with-blank-model", BackfillHunterModelIdentifier.HUNTER_AGENT, " ");

    underTest.execute();
    underTest.execute();

    Map<String, Object> hunterMapping = db.select("select model_identifier from llm_provider_mappings where id = 'hunter-with-blank-model'").getFirst();
    assertThat(hunterMapping).containsEntry("MODEL_IDENTIFIER", BackfillHunterModelIdentifier.HUNTER_MODEL_IDENTIFIER);
  }

  private void insertMapping(String id, String capability, String modelIdentifier) {
    db.executeInsert("llm_provider_mappings",
      "id", id,
      "ai_capability", capability,
      "llm_provider_id", "provider",
      "model_identifier", modelIdentifier,
      "created_at", 0L,
      "updated_at", 0L);
  }
}
