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
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateQGateUuidColumnForQGateConditionsTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateQGateUuidColumnForQGateConditionsTest.class, "schema.sql");

  private DataChange underTest = new PopulateQGateUuidColumnForQGateConditions(db.database());

  @Test
  public void populate_qgate_uuids() throws SQLException {
    insertQualityGate(1L, "uuid1", "qualityGate1");
    insertQualityGate(2L, "uuid2", "qualityGate2");
    insertQualityGate(3L, "uuid3", "qualityGate3");
    insertQualityGateCondition("condition_uuid_1", "uuid1");
    insertQualityGateCondition("condition_uuid_2", "uuid1");
    insertQualityGateCondition("condition_uuid_3", "uuid2");

    underTest.execute();

    verifyUuid("condition_uuid_1", "uuid1");
    verifyUuid("condition_uuid_2", "uuid1");
    verifyUuid("condition_uuid_3", "uuid2");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertQualityGate(1L, "uuid1", "qualityGate1");
    insertQualityGate(2L, "uuid2", "qualityGate2");
    insertQualityGate(3L, "uuid3", "qualityGate3");
    insertQualityGateCondition("condition_uuid_1", "uuid1");
    insertQualityGateCondition("condition_uuid_2", "uuid1");
    insertQualityGateCondition("condition_uuid_3", "uuid2");

    underTest.execute();
    // re-entrant
    underTest.execute();

    verifyUuid("condition_uuid_1", "uuid1");
    verifyUuid("condition_uuid_2", "uuid1");
    verifyUuid("condition_uuid_3", "uuid2");
  }

  private void verifyUuid(String conditionUuid, String expectedUuid) {
    assertThat(db.select("select QGATE_UUID from quality_gate_conditions where uuid='" + conditionUuid+"'")
      .stream()
      .map(row -> row.get("QGATE_UUID"))
      .collect(Collectors.toList())).containsOnly(expectedUuid);
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
