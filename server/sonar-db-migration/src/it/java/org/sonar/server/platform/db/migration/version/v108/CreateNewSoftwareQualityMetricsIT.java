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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CreateNewSoftwareQualityMetricsIT {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateNewSoftwareQualityMetrics.class);
  private final CreateNewSoftwareQualityMetrics underTest = new CreateNewSoftwareQualityMetrics(db.database(), UuidFactoryImpl.INSTANCE);

  @Test
  void execute_shouldCreateMetrics() throws SQLException {
    assertThat(db.select("select name,direction,qualitative,enabled,best_value,optimized_best_value,delete_historical_data  from metrics"))
      .isEmpty();
    underTest.execute();
    assertThat(db.select("select name,direction,qualitative,enabled,best_value,optimized_best_value,delete_historical_data  from metrics"))
      .hasSize(6)
      .extracting(s -> s.get("name"), s -> s.get("direction"), s -> s.get("qualitative"), s -> s.get("enabled"), s -> ((Number) s.get("best_value")).longValue(),
        s -> s.get("optimized_best_value"),
        s -> s.get("delete_historical_data"))

      .containsExactlyInAnyOrder(
        tuple("software_quality_reliability_issues", -1L, false, true, 0L, true, false),
        tuple("software_quality_security_issues", -1L, false, true, 0L, true, false),
        tuple("new_software_quality_reliability_issues", -1L, true, true, 0L, true, true),
        tuple("new_software_quality_security_issues", -1L, true, true, 0L, true, true),
        tuple("new_software_quality_maintainability_issues", -1L, true, true, 0L, true, true),
        tuple("software_quality_maintainability_issues", -1L, false, true, 0L, true, false));
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();
    assertThat(db.select("select name,direction,qualitative,enabled,best_value,optimized_best_value,delete_historical_data  from metrics"))
      .hasSize(6);
  }

  @Test
  void execute_whenOnlyOneMetricExists_shouldCreateOtherOnes() throws SQLException {
    String existingMetricUuid = insertMetric("software_quality_security_issues");
    underTest.execute();

    assertThat(db.select("select uuid  from metrics"))
      .hasSize(6)
      .extracting(e -> e.get("uuid"))
      .contains(existingMetricUuid);
  }

  private String insertMetric(String key) {
    String uuid = UuidFactoryImpl.INSTANCE.create();
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", uuid),
      Map.entry("NAME", key));
    db.executeInsert("metrics", map);
    return uuid;
  }

}
