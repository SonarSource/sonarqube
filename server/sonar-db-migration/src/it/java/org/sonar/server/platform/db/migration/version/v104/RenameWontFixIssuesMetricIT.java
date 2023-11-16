/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;


public class RenameWontFixIssuesMetricIT {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RenameWontFixIssuesMetric.class);
  private final RenameWontFixIssuesMetric underTest = new RenameWontFixIssuesMetric(db.database());

  @Test
  public void execute_whenMetricsTableIsEmpty_shouldDoNothing() throws SQLException {
    underTest.execute();

    assertThat(db.select("select name from metrics")).isEmpty();
  }

  @Test
  public void execute_whenWontFixMetricExist_shouldRenameToAccepted() throws SQLException {
    insertMetric("wont_fix_issues");
    insertMetric("other_metric");

    underTest.execute();

    assertThat(db.select("select name from metrics"))
      .extracting(stringObjectMap -> stringObjectMap.get("NAME"))
      .containsExactlyInAnyOrder("accepted_issues", "other_metric");
  }

  @Test
  public void execute_isReentrant() throws SQLException {
    insertMetric("wont_fix_issues");
    insertMetric("other_metric");

    underTest.execute();
    underTest.execute();

    assertThat(db.select("select name from metrics"))
      .extracting(stringObjectMap -> stringObjectMap.get("NAME"))
      .containsExactlyInAnyOrder("accepted_issues", "other_metric");
  }

  private void insertMetric(String name) {
    db.executeInsert("metrics",
      "NAME", name,
      "DESCRIPTION", "description " + name,
      "DIRECTION", -1,
      "DOMAIN", "Issues",
      "SHORT_NAME", name,
      "QUALITATIVE", true,
      "VAL_TYPE", "int",
      "ENABLED", true,
      "OPTIMIZED_BEST_VALUE", true,
      "HIDDEN", false,
      "DELETE_HISTORICAL_DATA", false,
      "UUID", name);
  }
}
