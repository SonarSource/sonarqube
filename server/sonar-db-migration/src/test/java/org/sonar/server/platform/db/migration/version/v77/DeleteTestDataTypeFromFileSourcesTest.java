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
package org.sonar.server.platform.db.migration.version.v77;

import java.sql.SQLException;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class DeleteTestDataTypeFromFileSourcesTest {

  private static final String TABLE = "file_sources";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteTestDataTypeFromFileSourcesTest.class, "file_sources.sql");

  private DeleteTestDataTypeFromFileSources underTest = new DeleteTestDataTypeFromFileSources(db.database());

  @Test
  public void remove_test_data_type() throws SQLException {
    insert("FILE1", "PROJECT1", "TEST");
    insert("FILE1", "PROJECT1", "SOURCE");
    insert("FILE2", "PROJECT2", "TEST");

    underTest.execute();

    assertFileSources(tuple("FILE1", "SOURCE"));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insert("FILE1", "PROJECT1", "SOURCE");
    insert("FILE1", "PROJECT1", "TEST");
    underTest.execute();

    underTest.execute();

    assertFileSources(tuple("FILE1", "SOURCE"));
  }

  private void assertFileSources(Tuple... expectedTuples) {
    assertThat(db.select("SELECT FILE_UUID, DATA_TYPE FROM FILE_SOURCES")
      .stream()
      .map(map -> new Tuple(map.get("FILE_UUID"), map.get("DATA_TYPE")))
      .collect(toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  public void insert(String fileUuid, String projectUuid, String dataType) {
    db.executeInsert(
      TABLE,
      "PROJECT_UUID", projectUuid,
      "DATA_TYPE", dataType,
      "FILE_UUID", fileUuid,
      "LINE_COUNT", 10,
      "CREATED_AT", 123456,
      "UPDATED_AT", 987654);
  }
}
