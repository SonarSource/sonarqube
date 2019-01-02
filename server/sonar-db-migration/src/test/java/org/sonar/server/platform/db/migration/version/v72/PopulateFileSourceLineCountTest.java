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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import java.util.Random;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateFileSourceLineCountTest {
  private static final String TABLE_NAME = "file_sources";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateFileSourceLineCountTest.class, "file_sources.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PopulateFileSourceLineCount underTest = new PopulateFileSourceLineCount(dbTester.database());

  @Test
  public void execute_does_not_fail_on_empty_table() throws SQLException {
    underTest.execute();
  }

  @Test
  public void execute_set_value_to_minus_1_when_line_count_is_null() throws SQLException {
    IntStream.range(0, 5 + new Random().nextInt(10)).forEach(i -> insert("prj_" + i, "file_" + i));

    underTest.execute();

    assertThat(dbTester.select("select distinct line_count as \"count\" from " + TABLE_NAME))
      .extracting(t -> t.get("count"))
      .containsOnly(-1L);
  }

  @Test
  public void execute_keeps_value_when_line_count_is_not_null() throws SQLException {
    insert("prj_A", "file_1", 12);
    insert("prj_A", "file_2", 0);
    insert("prj_B", "file_3", -5);
    insert("prj_B", "file_4", -5);
    insert("prj_C", "file_5", null);
    insert("prj_D", "file_6", null);
    insert("prj_D", "file_7", 12);

    underTest.execute();

    assertThat(dbTester.select("select line_count as \"count\" from " + TABLE_NAME))
      .extracting(t -> t.get("count"))
      .containsOnly(-1L, 12L, 12L, 0L, -5L, -5L);
  }

  public void insert(String projectUuid, String fileUuid) {
    insert(projectUuid, fileUuid, null);
  }

  public void insert(String projectUuid, String fileUuid, @Nullable Integer lineCount) {
    dbTester.executeInsert(
      TABLE_NAME,
      "PROJECT_UUID", projectUuid,
      "FILE_UUID", fileUuid,
      "LINE_COUNT", lineCount,
      "CREATED_AT", 123456,
      "UPDATED_AT", 987654);
  }
}
