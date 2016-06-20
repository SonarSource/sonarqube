/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v60;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class MakeComponentUuidNotNullOnDuplicationsIndexTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, MakeComponentUuidNotNullOnDuplicationsIndexTest.class,
    "in_progress_duplications_index.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeComponentUuidNotNullOnDuplicationsIndex underTest = new MakeComponentUuidNotNullOnDuplicationsIndex(db.database());

  @Test
  public void migration_sets_uuid_columns_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumnDefinitions();
  }

  @Test
  public void migration_sets_uuid_columns_not_nullable_on_populated_table() throws SQLException {
    insertDuplicationIndex(1L, true);
    insertDuplicationIndex(2L, true);

    underTest.execute();

    verifyColumnDefinitions();
    assertThat(idsOfRowsInDuplicationsIndex()).containsOnly(1L, 2L);
  }

  @Test
  public void migration_fails_if_some_uuid_columns_are_null() throws SQLException {
    insertDuplicationIndex(1L, false);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }

  private void verifyColumnDefinitions() {
    db.assertColumnDefinition("duplications_index", "component_uuid", Types.VARCHAR, 50, false);
  }

  private List<Long> idsOfRowsInDuplicationsIndex() {
    return db.select("select ID from duplications_index").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());
  }

  private void insertDuplicationIndex(long id, boolean hasComponentUuid) {
    db.executeInsert(
      "duplications_index",
      "ID", valueOf(id),
      "PROJECT_SNAPSHOT_ID", valueOf(10 + id),
      "SNAPSHOT_ID", valueOf(20 + id),
      "COMPONENT_UUID", hasComponentUuid ? valueOf(30 + id) : null,
      "HASH", "some_hash_" + id,
      "INDEX_IN_FILE", "2",
      "START_LINE", "3",
      "END_LINE", "4");
  }

}
