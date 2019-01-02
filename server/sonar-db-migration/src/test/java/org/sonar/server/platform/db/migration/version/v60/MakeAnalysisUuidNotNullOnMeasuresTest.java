/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.sql.Types;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;

public class MakeAnalysisUuidNotNullOnMeasuresTest {

  private static final String TABLE_MEASURES = "project_measures";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeAnalysisUuidNotNullOnMeasuresTest.class,
    "in_progress_measures.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeAnalysisUuidNotNullOnMeasures underTest = new MakeAnalysisUuidNotNullOnMeasures(db.database());

  @Test
  public void migration_makes_analysis_uuid_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumnDefinitions();
  }

  @Test
  public void migration_makes_analysis_uuid_not_nullable_on_populated_table() throws SQLException {
    insertMeasure(1, "U1");
    insertMeasure(2, "U2");

    underTest.execute();

    verifyColumnDefinitions();
  }

  @Test
  public void migration_fails_if_some_uuid_columns_are_null() throws SQLException {
    insertMeasure(2, null);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
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

  private void verifyColumnDefinitions() {
    db.assertColumnDefinition(TABLE_MEASURES, "analysis_uuid", Types.VARCHAR, 50, false);
  }

}
