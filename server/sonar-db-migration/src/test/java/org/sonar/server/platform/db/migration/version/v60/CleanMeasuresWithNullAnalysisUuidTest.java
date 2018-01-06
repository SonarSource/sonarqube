/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanMeasuresWithNullAnalysisUuidTest {

  private static final String TABLE_MEASURES = "project_measures";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CleanMeasuresWithNullAnalysisUuidTest.class,
    "in_progress_measures.sql");

  private CleanMeasuresWithNullAnalysisUuid underTest = new CleanMeasuresWithNullAnalysisUuid(db.database());

  @Test
  public void migration_has_no_effect_on_empty_table() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(0);
  }

  @Test
  public void migration_deletes_rows_with_null_analysis_uuid() throws SQLException {
    insertMeasure(1, "U1");
    insertMeasure(2, "U1");
    insertMeasure(3, null);

    underTest.execute();

    assertThat(idsOfRows()).containsOnly(1L, 2L);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertMeasure(1, "U1");
    insertMeasure(2, null);

    underTest.execute();
    assertThat(idsOfRows()).containsOnly(1L);

    underTest.execute();
    assertThat(idsOfRows()).containsOnly(1L);
  }

  private void insertMeasure(long id, @Nullable String analysisUuid) {
    db.executeInsert(
      TABLE_MEASURES,
      "ID", valueOf(id),
      "SNAPSHOT_ID", valueOf(id + 10),
      "METRIC_ID", valueOf(id + 100),
      "VALUE", valueOf(id + 200),
      "COMPONENT_UUID", valueOf(id + 300),
      "ANALYSIS_UUID", analysisUuid);
  }

  private List<Long> idsOfRows() {
    return db.select("select ID from project_measures").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());
  }
}
