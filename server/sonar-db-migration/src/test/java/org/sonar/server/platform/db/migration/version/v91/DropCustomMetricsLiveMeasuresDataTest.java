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
package org.sonar.server.platform.db.migration.version.v91;

import java.sql.SQLException;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class DropCustomMetricsLiveMeasuresDataTest {
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(DropCustomMetricsLiveMeasuresDataTest.class, "schema.sql");

  @Rule
  public final CoreDbTester dbWithoutColumn = CoreDbTester.createForSchema(DropCustomMetricsLiveMeasuresDataTest.class, "no_user_managed_column.sql");

  private final DataChange underTest = new DropCustomMetricsLiveMeasuresData(db.database());

  @Test
  public void do_not_fail_if_no_rows_to_delete() {
    assertThatCode(underTest::execute)
      .doesNotThrowAnyException();
  }

  @Test
  public void delete_user_managed_live_measures_when_other_measures_exist() throws SQLException {
    insertMetric("metric-1", true);
    insertMetric("metric-2", true);
    insertMetric("metric-3", false);

    insertLiveMeasure("lm-1", "metric-1");
    insertLiveMeasure("lm-2", "metric-1");
    insertLiveMeasure("lm-3", "metric-2");
    insertLiveMeasure("lm-4", "metric-2");
    insertLiveMeasure("lm-5", "metric-3");
    insertLiveMeasure("lm-6", "metric-3");

    underTest.execute();

    // re-entrant
    underTest.execute();

    assertThat(db.select("select uuid from live_measures").stream().map(row -> row.get("UUID")).collect(Collectors.toList()))
      .containsExactlyInAnyOrder("lm-5", "lm-6");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertMetric("metric-1", true);
    insertMetric("metric-2", true);
    insertMetric("metric-3", false);

    insertLiveMeasure("lm-1", "metric-1");
    insertLiveMeasure("lm-2", "metric-1");
    insertLiveMeasure("lm-3", "metric-2");
    insertLiveMeasure("lm-4", "metric-2");
    insertLiveMeasure("lm-5", "metric-3");
    insertLiveMeasure("lm-6", "metric-3");

    underTest.execute();

    // re-entrant
    underTest.execute();

    assertThat(db.select("select uuid from live_measures").stream().map(row -> row.get("UUID")).collect(Collectors.toList()))
      .containsExactlyInAnyOrder("lm-5", "lm-6");
  }

  @Test
  public void delete_user_managed_metrics() throws SQLException {
    insertMetric("metric-1", true);
    insertMetric("metric-2", true);

    insertLiveMeasure("lm-1", "metric-1");
    insertLiveMeasure("lm-2", "metric-1");
    insertLiveMeasure("lm-3", "metric-2");
    insertLiveMeasure("lm-4", "metric-2");

    underTest.execute();

    // re-entrant
    underTest.execute();

    assertThat(db.select("select uuid from live_measures").stream().map(row -> row.get("UUID")).collect(Collectors.toList()))
      .isEmpty();
  }

  @Test
  public void do_not_fail_if_no_user_managed_rows_to_delete() throws SQLException {
    insertMetric("metric-1", false);
    insertMetric("metric-2", false);

    insertLiveMeasure("lm-1", "metric-1");
    insertLiveMeasure("lm-2", "metric-1");
    insertLiveMeasure("lm-3", "metric-2");
    insertLiveMeasure("lm-4", "metric-2");

    underTest.execute();

    assertThat(db.select("select uuid from live_measures").stream().map(row -> row.get("UUID")).collect(Collectors.toList()))
      .containsExactlyInAnyOrder("lm-1", "lm-2", "lm-3", "lm-4");
  }

  @Test
  public void does_not_fail_when_no_user_managed_column() throws SQLException {
    insertMetric("metric-1", false);
    insertMetric("metric-2", false);

    insertLiveMeasure("lm-1", "metric-1");
    insertLiveMeasure("lm-2", "metric-1");
    insertLiveMeasure("lm-3", "metric-2");
    insertLiveMeasure("lm-4", "metric-2");

    DataChange underTest = new DropCustomMetricsLiveMeasuresData(dbWithoutColumn.database());
    underTest.execute();

    assertThat(db.select("select uuid from live_measures").stream().map(row -> row.get("UUID")).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("lm-1", "lm-2", "lm-3", "lm-4");
  }

  private void insertLiveMeasure(String uuid, String metricUuid) {
    db.executeInsert("live_measures",
      "UUID", uuid,
      "PROJECT_UUID", uuidFactory.create(),
      "COMPONENT_UUID", uuidFactory.create(),
      "METRIC_UUID", metricUuid,
      "CREATED_AT", System.currentTimeMillis(),
      "UPDATED_AT", System.currentTimeMillis());
  }

  private void insertMetric(String uuid, boolean userManaged) {
    db.executeInsert("metrics",
      "UUID", uuid,
      "NAME", "name-" + uuid,
      "USER_MANAGED", String.valueOf(userManaged));
  }
}
