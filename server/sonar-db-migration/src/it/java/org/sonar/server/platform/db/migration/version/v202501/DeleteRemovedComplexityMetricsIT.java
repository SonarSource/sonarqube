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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteRemovedComplexityMetricsIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DeleteRemovedComplexityMetrics.class);

  private final DeleteRemovedComplexityMetrics underTest = new DeleteRemovedComplexityMetrics(db.database());

  @Test
  void execute_whenMetricsExists_shouldDeleteMetrics() throws SQLException {
    DeleteRemovedComplexityMeasuresFromProjectMeasures.COMPLEXITY_METRICS_TO_DELETE.forEach(this::insertMetric);

    assertThat(db.countSql("select count(1) from metrics"))
      .isEqualTo(DeleteRemovedComplexityMeasuresFromProjectMeasures.COMPLEXITY_METRICS_TO_DELETE.size());

    underTest.execute();

    assertThat(db.countSql("select count(1) from metrics")).isZero();
  }

  @Test
  void execute_whenOtherMetricExists_shouldNotDeleteMetric() throws SQLException {
    insertMetric("other_metric");

    assertThat(db.countSql("select count(1) from metrics")).isEqualTo(1);

    underTest.execute();

    assertThat(db.countSql("select count(1) from metrics")).isEqualTo(1);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    DeleteRemovedComplexityMeasuresFromProjectMeasures.COMPLEXITY_METRICS_TO_DELETE.forEach(this::insertMetric);

    insertMetric("other_metric");

    assertThat(db.countSql("select count(1) from metrics"))
      .isEqualTo(DeleteRemovedComplexityMeasuresFromProjectMeasures.COMPLEXITY_METRICS_TO_DELETE.size() + 1);

    underTest.execute();
    underTest.execute();

    assertThat(db.countSql("select count(1) from metrics")).isOne();
  }

  private void insertMetric(String key) {
    String uuid = UuidFactoryImpl.INSTANCE.create();
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", uuid),
      Map.entry("NAME", key));
    db.executeInsert("metrics", map);
  }
}
