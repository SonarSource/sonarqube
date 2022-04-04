/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.qualitygates;

import java.sql.SQLException;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class DropOrphansQGateConditionsTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropOrphansQGateConditionsTest.class, "schema.sql");

  private MigrationStep underTest = new DropOrphansQGateConditions(db.database());

  @Test
  public void deleteOrphanQGConditions() throws SQLException {
    insertQualityGate(1L, "uuid1", "qualityGate1");
    insertQualityGateCondition("condition_uuid_1", "uuid1");
    insertQualityGateCondition("condition_uuid_2", "uuid1");
    insertQualityGateCondition("condition_uuid_3", null);

    underTest.execute();

    verifyConditionExist("condition_uuid_1", true);
    verifyConditionExist("condition_uuid_2", true);
    verifyConditionExist("condition_uuid_3", false);
  }

  private void verifyConditionExist(String uuid, boolean exist) {
    assertThat(db.select("select count(uuid) as C from quality_gate_conditions where uuid='" + uuid + "'")
      .stream()
      .map(row -> (Long) row.get("C"))
      .collect(Collectors.toList())).containsOnly(exist ? 1L : 0L);
  }

  private void insertQualityGate(Long id, String uuid, String name) {
    db.executeInsert("quality_gates",
      "id", id,
      "uuid", uuid,
      "name", name,
      "is_built_in", true);
  }

  private void insertQualityGateCondition(String uuid, String qualityGateUuid) {
    db.executeInsert("quality_gate_conditions",
      "uuid", uuid,
      "qgate_uuid", qualityGateUuid);
  }

}
