/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.source.index;

import org.assertj.core.data.MapEntry;
import org.elasticsearch.action.update.UpdateRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.db.FileSourceTesting;
import org.sonar.test.DbTests;

import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@Category(DbTests.class)
public class SourceLineResultSetIteratorTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(SourceLineResultSetIteratorTest.class, "schema.sql");

  DbClient dbClient;

  Connection connection;

  SourceLineResultSetIterator iterator;

  @Before
  public void setUp() throws Exception {
    dbClient = new DbClient(db.database(), db.myBatis());
    connection = db.openConnection();
  }

  @After
  public void after() throws Exception {
    if (iterator != null) {
      iterator.close();
    }
    connection.close();
  }

  @Test
  public void traverse_db() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    FileSourceTesting.updateDataColumn(connection, "F1", FileSourceTesting.newFakeData(3).build());

    iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L, null);
    assertThat(iterator.hasNext()).isTrue();
    FileSourcesUpdaterHelper.Row row = iterator.next();
    assertThat(row.getProjectUuid()).isEqualTo("P1");
    assertThat(row.getFileUuid()).isEqualTo("F1");
    assertThat(row.getUpdatedAt()).isEqualTo(1416239042000L);
    assertThat(row.getUpdateRequests()).hasSize(3);

    UpdateRequest firstRequest = row.getUpdateRequests().get(0);
    Map<String, Object> doc = firstRequest.doc().sourceAsMap();
    assertThat(doc).contains(
      MapEntry.entry(SourceLineIndexDefinition.FIELD_PROJECT_UUID, "P1"),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_FILE_UUID, "F1"),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_LINE, 1),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_SCM_REVISION, "REVISION_1"),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_SCM_AUTHOR, "AUTHOR_1"),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_HIGHLIGHTING, "HIGHLIGHTING_1"),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_SYMBOLS, "SYMBOLS_1"),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_UT_LINE_HITS, 1),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_UT_CONDITIONS, 2),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_UT_COVERED_CONDITIONS, 3),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_IT_LINE_HITS, 4),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_IT_CONDITIONS, 5),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_IT_COVERED_CONDITIONS, 6),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_OVERALL_LINE_HITS, 7),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_OVERALL_CONDITIONS, 8),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_OVERALL_COVERED_CONDITIONS, 9)
    );
  }

  /**
   * File with one line. No metadata available on the line.
   */
  @Test
  public void minimal_data() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    FileSourceDb.Data.Builder dataBuilder = FileSourceDb.Data.newBuilder();
    dataBuilder.addLinesBuilder().setLine(1).build();
    FileSourceTesting.updateDataColumn(connection, "F1", dataBuilder.build());

    iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L, null);
    FileSourcesUpdaterHelper.Row row = iterator.next();
    assertThat(row.getProjectUuid()).isEqualTo("P1");
    assertThat(row.getFileUuid()).isEqualTo("F1");
    assertThat(row.getUpdatedAt()).isEqualTo(1416239042000L);
    assertThat(row.getUpdateRequests()).hasSize(1);
    UpdateRequest firstRequest = row.getUpdateRequests().get(0);
    Map<String, Object> doc = firstRequest.doc().sourceAsMap();
    assertThat(doc).contains(
      MapEntry.entry(SourceLineIndexDefinition.FIELD_PROJECT_UUID, "P1"),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_FILE_UUID, "F1"),
      MapEntry.entry(SourceLineIndexDefinition.FIELD_LINE, 1)
    );
    // null values
    assertThat(doc).containsKeys(
      SourceLineIndexDefinition.FIELD_SCM_REVISION,
      SourceLineIndexDefinition.FIELD_SCM_AUTHOR,
      SourceLineIndexDefinition.FIELD_HIGHLIGHTING,
      SourceLineIndexDefinition.FIELD_SYMBOLS,
      SourceLineIndexDefinition.FIELD_UT_LINE_HITS,
      SourceLineIndexDefinition.FIELD_UT_CONDITIONS,
      SourceLineIndexDefinition.FIELD_UT_COVERED_CONDITIONS,
      SourceLineIndexDefinition.FIELD_IT_LINE_HITS,
      SourceLineIndexDefinition.FIELD_IT_CONDITIONS,
      SourceLineIndexDefinition.FIELD_IT_COVERED_CONDITIONS,
      SourceLineIndexDefinition.FIELD_OVERALL_LINE_HITS,
      SourceLineIndexDefinition.FIELD_OVERALL_CONDITIONS,
      SourceLineIndexDefinition.FIELD_OVERALL_COVERED_CONDITIONS
    );
  }

  @Test
  public void filter_by_date() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    iterator = SourceLineResultSetIterator.create(dbClient, connection, 2000000000000L, null);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void filter_by_project() throws Exception {
    db.prepareDbUnit(getClass(), "filter_by_project.xml");
    FileSourceDb.Data.Builder dataBuilder = FileSourceDb.Data.newBuilder();
    dataBuilder.addLinesBuilder().setLine(1).build();
    FileSourceTesting.updateDataColumn(connection, "F1", dataBuilder.build());

    iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L, "P1");

    FileSourcesUpdaterHelper.Row row = iterator.next();
    assertThat(row.getProjectUuid()).isEqualTo("P1");
    assertThat(row.getFileUuid()).isEqualTo("F1");

    // File from other project P2 is not returned
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void filter_by_project_and_date() throws Exception {
    db.prepareDbUnit(getClass(), "filter_by_project_and_date.xml");
    FileSourceDb.Data.Builder dataBuilder = FileSourceDb.Data.newBuilder();
    dataBuilder.addLinesBuilder().setLine(1).build();
    FileSourceTesting.updateDataColumn(connection, "F1", dataBuilder.build());

    iterator = SourceLineResultSetIterator.create(dbClient, connection, 1400000000000L, "P1");

    FileSourcesUpdaterHelper.Row row = iterator.next();
    assertThat(row.getProjectUuid()).isEqualTo("P1");
    assertThat(row.getFileUuid()).isEqualTo("F1");

    // File F2 is not returned
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void fail_on_bad_data_format() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    FileSourceTesting.updateDataColumn(connection, "F1", "THIS_IS_NOT_PROTOBUF".getBytes());

    iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L, null);
    try {
      assertThat(iterator.hasNext()).isTrue();
      iterator.next();
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }
}
