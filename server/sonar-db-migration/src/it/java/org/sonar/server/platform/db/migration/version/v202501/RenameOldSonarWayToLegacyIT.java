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
package org.sonar.server.platform.db.migration.version.v202501;

import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

class RenameOldSonarWayToLegacyIT {
  @RegisterExtension
  final LogTesterJUnit5 logger = new LogTesterJUnit5();

  @RegisterExtension
  final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RenameOldSonarWayToLegacy.class);

  private final DataChange underTest = new RenameOldSonarWayToLegacy(db.database());

  @BeforeEach
  void before() {
    logger.clear();
  }

  @Test
  void migration_when_empty_sonar_way_exists() throws SQLException {
    db.executeInsert("quality_gates", "name", "Sonar way", "is_built_in", true, "uuid", "quality_gate_uuid");

    underTest.execute();

    assertThat(db.select("select * from quality_gates where name='Sonar way'")).isNotEmpty();
    assertThat(db.select("select * from quality_gates where name='Sonar way (legacy)'")).isEmpty();
  }

  @Test
  void migration_when_old_sonar_way_exists() throws SQLException {
    insertOldSonarWay();

    underTest.execute();

    assertThat(db.select("select * from quality_gates where name='Sonar way'")).isEmpty();
    Map<String, Object> legacy = db.selectFirst("select * from quality_gates where name='Sonar way (legacy)'");
    assertThat(legacy).isNotEmpty().contains(Map.entry("is_built_in", false));
  }

  @Test
  void migration_when_new_sonar_way_exists() throws SQLException {
    insertNewSonarWay();

    underTest.execute();

    assertThat(db.select("select * from quality_gates where name='Sonar way'")).isNotEmpty();
    assertThat(db.select("select * from quality_gates where name='Sonar way (legacy)'")).isEmpty();
  }

  @Test
  void migration_when_old_sonar_way_exists_is_reentrant() throws SQLException {
    insertOldSonarWay();

    underTest.execute();
    underTest.execute();

    assertThat(db.select("select * from quality_gates where name='Sonar way'")).isEmpty();
    Map<String, Object> legacy = db.selectFirst("select * from quality_gates where name='Sonar way (legacy)'");
    assertThat(legacy).isNotEmpty().contains(Map.entry("is_built_in", false));
  }

  private void insertOldSonarWay() {
    insertSonarWay("some_metric");
  }

  private void insertSonarWay(String metric) {
    db.executeInsert("quality_gates", "name", "Sonar way", "is_built_in", true, "uuid", "quality_gate_uuid");
    db.executeInsert("METRICS", "name", metric, "uuid", "metric_uuid");
    db.executeInsert("quality_gate_conditions", "metric_uuid", "metric_uuid", "qgate_uuid", "quality_gate_uuid", "value_error", "0",
      "operator", "GT", "uuid", "condition_uuid");
  }

  private void insertNewSonarWay() {
    insertSonarWay("new_violations");
  }

}
