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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class MakeCeActivityExecutionCountNotNullableTest {

  private static final String TABLE_CE_ACTIVITY = "ce_activity";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeCeActivityExecutionCountNotNullableTest.class, "ce_activity.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeCeActivityExecutionCountNotNullable underTest = new MakeCeActivityExecutionCountNotNullable(db.database());

  @Test
  public void execute_makes_column_execution_count_not_nullable_when_table_is_empty() throws SQLException {
    underTest.execute();

    verifyColumnDefinition();
  }

  @Test
  public void execute_set_column_execution_count_to_0_or_1_and_not_nullable_depending_on_status_of_the_task() throws SQLException {
    insertCeActivity("u1", Status.SUCCESS);
    insertCeActivity("u2", Status.FAILED);
    insertCeActivity("u3", Status.CANCELED);

    underTest.execute();

    verifyColumnDefinition();
    assertThat(getUuidsForExecutionCount(0)).containsOnly("u3");
    assertThat(getUuidsForExecutionCount(1)).containsOnly("u1", "u2");
  }

  private List<Object> getUuidsForExecutionCount(int executionCount) {
    return db.select("select uuid as \"UUID\" from ce_activity where execution_count=" + executionCount)
      .stream()
      .flatMap(row -> Stream.of(row.get("UUID")))
      .collect(MoreCollectors.toList());
  }

  private void verifyColumnDefinition() {
    db.assertColumnDefinition(TABLE_CE_ACTIVITY, "execution_count", Types.INTEGER, null, false);
  }

  private void insertCeActivity(String uuid, Status status) {
    db.executeInsert(TABLE_CE_ACTIVITY,
      "UUID", uuid,
      "TASK_TYPE", uuid + "_type",
      "STATUS", status.name(),
      "IS_LAST", new Random().nextBoolean() + "",
      "IS_LAST_KEY", "key",
      "SUBMITTED_AT", new Random().nextLong() + "",
      "CREATED_AT", new Random().nextLong() + "",
      "UPDATED_AT", new Random().nextLong() + "");
  }

  public enum Status {
    SUCCESS, FAILED, CANCELED
  }

}
