/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.metrics.livemeasures;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateLiveMeasuresMetricUuidTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateLiveMeasuresMetricUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateLiveMeasuresMetricUuid(db.database());

  @Test
  public void populate_uuids() throws SQLException {
    insertMetric(1L);
    insertMetric(2L);
    insertMetric(3L);

    insertLiveMeasure(4L, 1L);
    insertLiveMeasure(5L, 2L);
    insertLiveMeasure(6L, 3L);

    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", 1L, "uuid1"),
      tuple("uuid5", 2L, "uuid2"),
      tuple("uuid6", 3L, "uuid3")
    );
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertMetric(1L);
    insertMetric(2L);
    insertMetric(3L);

    insertLiveMeasure(4L, 1L);
    insertLiveMeasure(5L, 2L);
    insertLiveMeasure(6L, 3L);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", 1L, "uuid1"),
      tuple("uuid5", 2L, "uuid2"),
      tuple("uuid6", 3L, "uuid3")
    );
  }

  private void assertThatTableContains(Tuple... tuples) {
    List<Map<String, Object>> select = db.select("select uuid, metric_id, metric_uuid from live_measures");
    assertThat(select).extracting(m -> m.get("UUID"), m -> m.get("METRIC_ID"), m -> m.get("METRIC_UUID"))
      .containsExactlyInAnyOrder(tuples);
  }

  private void insertMetric(Long id) {
    db.executeInsert("metrics",
      "id", id,
      "uuid", "uuid" + id,
      "name", "name" + id);
  }

  private void insertLiveMeasure(Long id, Long metricId) {
    db.executeInsert("live_measures",
      "uuid", "uuid" + id,
      "metric_id", metricId,
      "component_uuid", "component" + id,
      "project_uuid", "project" + id + 2,
      "created_at", id + 3,
      "updated_at", id + 4);
  }
}
