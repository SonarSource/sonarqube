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

public class MakeCeQueueExecutionCountNotNullableTest {

  private static final String TABLE_CE_QUEUE = "ce_queue";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeCeQueueExecutionCountNotNullableTest.class, "ce_queue.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeCeQueueExecutionCountNotNullable underTest = new MakeCeQueueExecutionCountNotNullable(db.database());

  @Test
  public void execute_makes_column_execution_count_not_nullable_when_table_is_empty() throws SQLException {
    underTest.execute();

    verifyColumnDefinition();
  }

  @Test
  public void execute_set_column_execution_count_to_0_and_not_nullable_no_matter_status_of_the_task() throws SQLException {
    insertCeQueue("u1", Status.IN_PROGRESS);
    insertCeQueue("u2", Status.PENDING);

    underTest.execute();

    verifyColumnDefinition();
    assertThat(getUuidsForExecutionCount(0)).containsOnly("u1", "u2");
    assertThat(getUuidsForExecutionCount(1)).isEmpty();
  }

  private List<Object> getUuidsForExecutionCount(int executionCount) {
    return db.select("select uuid as \"UUID\" from ce_queue where execution_count=" + executionCount)
        .stream()
        .flatMap(row -> Stream.of(row.get("UUID")))
        .collect(MoreCollectors.toList());
  }

  private void verifyColumnDefinition() {
    db.assertColumnDefinition(TABLE_CE_QUEUE, "execution_count", Types.INTEGER, null, false);
  }

  private void insertCeQueue(String uuid, Status status) {
    db.executeInsert(TABLE_CE_QUEUE,
        "UUID", uuid,
        "TASK_TYPE", uuid + "_type",
        "STATUS", status.name(),
        "CREATED_AT", new Random().nextLong() + "",
        "UPDATED_AT", new Random().nextLong() + "");
  }

  public enum Status {
    PENDING, IN_PROGRESS
  }
}
