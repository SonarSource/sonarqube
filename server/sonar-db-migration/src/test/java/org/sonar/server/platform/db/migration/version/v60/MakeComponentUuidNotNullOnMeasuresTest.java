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
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class MakeComponentUuidNotNullOnMeasuresTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeComponentUuidNotNullOnMeasuresTest.class,
    "in_progress_project_measures.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeComponentUuidNotNullOnMeasures underTest = new MakeComponentUuidNotNullOnMeasures(db.database());

  @Test
  public void migration_sets_uuid_columns_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumnDefinitions();
  }

  @Test
  public void migration_sets_uuid_columns_not_nullable_on_populated_table() throws SQLException {
    insertMeasure(1L, true);
    insertMeasure(2L, true);

    underTest.execute();

    verifyColumnDefinitions();
    assertThat(idsOfRowsInMeasures()).containsOnly(1L, 2L);
  }

  @Test
  public void migration_fails_if_some_uuid_columns_are_null() throws SQLException {
    insertMeasure(1L, false);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }

  private void verifyColumnDefinitions() {
    db.assertColumnDefinition("project_measures", "component_uuid", Types.VARCHAR, 50, false);
  }

  private List<Long> idsOfRowsInMeasures() {
    return db.select("select ID from project_measures").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());
  }

  private void insertMeasure(long id, boolean hasComponentUuid) {
    db.executeInsert(
      "project_measures",
      "ID", valueOf(id),
      "METRIC_ID", valueOf(id + 10),
      "SNAPSHOT_ID", valueOf(id + 100),
      "VALUE", valueOf(id + 1000));

    if (hasComponentUuid) {
      db.executeUpdateSql("update project_measures set COMPONENT_UUID=? where id=?", "uuid_" + id, valueOf(id));
    }
  }
}
