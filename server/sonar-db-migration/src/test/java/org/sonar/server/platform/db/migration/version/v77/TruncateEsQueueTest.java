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
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class TruncateEsQueueTest {
  private static final String ES_QUEUE_TABLE = "es_queue";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(TruncateEsQueueTest.class, "es_queue.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TruncateEsQueue underTest = new TruncateEsQueue(db.database());

  @Test
  public void migration_does_not_fail_on_empty_table() throws SQLException {
    assertThat(db.countRowsOfTable(ES_QUEUE_TABLE)).isZero();

    underTest.execute();

    assertThat(db.countRowsOfTable(ES_QUEUE_TABLE)).isZero();
  }

  @Test
  public void migration_truncates_content_of_table_es_queue() throws SQLException {
    int count = insertData();
    assertThat(db.countRowsOfTable(ES_QUEUE_TABLE)).isEqualTo(count);

    underTest.execute();

    assertThat(db.countRowsOfTable(ES_QUEUE_TABLE)).isZero();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    int count = insertData();
    assertThat(db.countRowsOfTable(ES_QUEUE_TABLE)).isEqualTo(count);

    underTest.execute();
    assertThat(db.countRowsOfTable(ES_QUEUE_TABLE)).isZero();

    underTest.execute();
    assertThat(db.countRowsOfTable(ES_QUEUE_TABLE)).isZero();
  }

  private int insertData() {
    int count = 5 + new Random().nextInt(200);
    IntStream.range(0, count)
      .forEach(i -> db.executeInsert(
        ES_QUEUE_TABLE,
        "UUID", "uuid_" + i,
        "DOC_TYPE", "doc_type_" + i,
        "DOC_ID", "doc_id_" + i,
        "CREATED_AT", String.valueOf(i)
      ));
    return count;
  }
}
