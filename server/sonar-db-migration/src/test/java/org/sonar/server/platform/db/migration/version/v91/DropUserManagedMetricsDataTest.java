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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class DropUserManagedMetricsDataTest {
  private static final String TABLE_NAME = "metrics";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(DropUserManagedMetricsDataTest.class, "schema.sql");

  @Rule
  public final CoreDbTester dbWithoutColumn = CoreDbTester.createForSchema(DropUserManagedMetricsDataTest.class, "no_user_managed_column.sql");

  private final DataChange underTest = new DropUserManagedMetricsData(db.database());

  @Test
  public void do_not_fail_if_no_rows_to_delete() {
    assertThatCode(underTest::execute)
      .doesNotThrowAnyException();
  }

  @Test
  public void delete_user_managed_metrics_when_other_metrics_exist() throws SQLException {
    insertMetric("1", false);
    insertMetric("2", false);
    insertMetric("3", false);

    insertMetric("4", true);
    insertMetric("5", true);
    insertMetric("6", true);

    underTest.execute();

    assertThat(db.countSql("select count(*) from metrics where user_managed = false")).isEqualTo(3);
    assertThat(db.countSql("select count(*) from metrics where user_managed = true")).isZero();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertMetric("1", false);
    insertMetric("2", false);
    insertMetric("3", false);

    insertMetric("4", true);
    insertMetric("5", true);
    insertMetric("6", true);

    underTest.execute();

    // re-entrant
    underTest.execute();

    assertThat(db.countSql("select count(*) from metrics where user_managed = false")).isEqualTo(3);
    assertThat(db.countSql("select count(*) from metrics where user_managed = true")).isZero();
  }

  @Test
  public void delete_user_managed_metrics() throws SQLException {
    insertMetric("1", true);
    insertMetric("2", true);
    insertMetric("3", true);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_NAME)).isZero();
  }

  @Test
  public void do_not_fail_if_no_user_managed_rows_to_delete() throws SQLException {
    insertMetric("1", false);
    insertMetric("2", false);
    insertMetric("3", false);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(3);
  }

  @Test
  public void does_not_fail_when_no_user_managed_column() throws SQLException {
    insertMetric("1", false);
    insertMetric("2", false);
    insertMetric("3", false);

    DataChange underTest = new DropCustomMetricsProjectMeasuresData(dbWithoutColumn.database());
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(3);
  }

  private void insertMetric(String uuid, boolean userManaged) {
    db.executeInsert(TABLE_NAME,
      "UUID", uuid,
      "NAME", "name-" + uuid,
      "USER_MANAGED", String.valueOf(userManaged));
  }

}
