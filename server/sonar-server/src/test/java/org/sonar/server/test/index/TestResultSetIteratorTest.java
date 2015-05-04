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

package org.sonar.server.test.index;

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
import org.sonar.server.source.db.FileSourceDb.Test.TestStatus;
import org.sonar.server.source.index.FileSourcesUpdaterHelper;
import org.sonar.server.source.index.SourceLineResultSetIteratorTest;
import org.sonar.server.test.db.TestTesting;
import org.sonar.test.DbTests;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Category(DbTests.class)
public class TestResultSetIteratorTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(SourceLineResultSetIteratorTest.class, "schema.sql");

  DbClient dbClient;

  Connection connection;

  TestResultSetIterator sut;

  @Before
  public void setUp() throws Exception {
    dbClient = new DbClient(db.database(), db.myBatis());
    connection = db.openConnection();
  }

  @After
  public void after() throws Exception {
    if (sut != null) {
      sut.close();
    }
    connection.close();
  }

  @Test
  public void traverse_db() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    TestTesting.updateDataColumn(connection, "F1", newFakeTests(3));
    sut = TestResultSetIterator.create(dbClient, connection, 0L, null);

    FileSourcesUpdaterHelper.Row row = sut.next();
    assertThat(row.getProjectUuid()).isEqualTo("P1");
    assertThat(row.getFileUuid()).isEqualTo("F1");
    assertThat(row.getUpdatedAt()).isEqualTo(1416239042000L);
    assertThat(row.getUpdateRequests()).hasSize(3);

    UpdateRequest firstRequest = row.getUpdateRequests().get(0);
    Map<String, Object> doc = firstRequest.doc().sourceAsMap();
    assertThat(doc).contains(
      MapEntry.entry(TestIndexDefinition.FIELD_PROJECT_UUID, "P1"),
      MapEntry.entry(TestIndexDefinition.FIELD_FILE_UUID, "F1"),
      MapEntry.entry(TestIndexDefinition.FIELD_TEST_UUID, "TEST_FILE_UUID_1"),
      MapEntry.entry(TestIndexDefinition.FIELD_STATUS, "FAILURE"),
      MapEntry.entry(TestIndexDefinition.FIELD_MESSAGE, "MESSAGE_1"),
      MapEntry.entry(TestIndexDefinition.FIELD_DURATION_IN_MS, 1),
      MapEntry.entry(TestIndexDefinition.FIELD_STACKTRACE, "STACKTRACE_1"),
      MapEntry.entry(TestIndexDefinition.FIELD_NAME, "NAME_1")
      );
  }

  /**
   * File with one line. No metadata available on the line.
   */
  @Test
  public void minimal_data() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    List<FileSourceDb.Test> tests = Arrays.asList(
      FileSourceDb.Test.newBuilder()
        .setUuid("U1")
        .setName("N1")
        .build()
      );
    TestTesting.updateDataColumn(connection, "F1", tests);
    sut = TestResultSetIterator.create(dbClient, connection, 0L, null);

    FileSourcesUpdaterHelper.Row row = sut.next();

    assertThat(row.getProjectUuid()).isEqualTo("P1");
    assertThat(row.getFileUuid()).isEqualTo("F1");
    assertThat(row.getUpdatedAt()).isEqualTo(1416239042000L);
    assertThat(row.getUpdateRequests()).hasSize(1);
    UpdateRequest firstRequest = row.getUpdateRequests().get(0);
    Map<String, Object> doc = firstRequest.doc().sourceAsMap();
    assertThat(doc).contains(
      MapEntry.entry(TestIndexDefinition.FIELD_PROJECT_UUID, "P1"),
      MapEntry.entry(TestIndexDefinition.FIELD_FILE_UUID, "F1"),
      MapEntry.entry(TestIndexDefinition.FIELD_TEST_UUID, "U1"),
      MapEntry.entry(TestIndexDefinition.FIELD_NAME, "N1")
      );
    // null values
    assertThat(doc).containsKeys(
      TestIndexDefinition.FIELD_DURATION_IN_MS,
      TestIndexDefinition.FIELD_STACKTRACE,
      TestIndexDefinition.FIELD_MESSAGE,
      TestIndexDefinition.FIELD_STATUS,
      TestIndexDefinition.FIELD_COVERED_FILES
      );
  }

  @Test
  public void filter_by_date() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    sut = TestResultSetIterator.create(dbClient, connection, 2000000000000L, null);

    assertThat(sut.hasNext()).isFalse();
  }

  @Test
  public void filter_by_project() throws Exception {
    db.prepareDbUnit(getClass(), "filter_by_project.xml");
    TestTesting.updateDataColumn(connection, "F1", newFakeTests(1));

    sut = TestResultSetIterator.create(dbClient, connection, 0L, "P1");

    FileSourcesUpdaterHelper.Row row = sut.next();
    assertThat(row.getProjectUuid()).isEqualTo("P1");
    assertThat(row.getFileUuid()).isEqualTo("F1");

    // File from other project P2 is not returned
    assertThat(sut.hasNext()).isFalse();
  }

  @Test
  public void filter_by_project_and_date() throws Exception {
    db.prepareDbUnit(getClass(), "filter_by_project_and_date.xml");
    TestTesting.updateDataColumn(connection, "F1", newFakeTests(1));

    sut = TestResultSetIterator.create(dbClient, connection, 1400000000000L, "P1");

    FileSourcesUpdaterHelper.Row row = sut.next();
    assertThat(row.getProjectUuid()).isEqualTo("P1");
    assertThat(row.getFileUuid()).isEqualTo("F1");

    // File F2 is not returned
    assertThat(sut.hasNext()).isFalse();
  }

  @Test
  public void fail_on_bad_data_format() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    TestTesting.updateDataColumn(connection, "F1", "THIS_IS_NOT_PROTOBUF".getBytes());

    sut = TestResultSetIterator.create(dbClient, connection, 0L, null);
    try {
      assertThat(sut.hasNext()).isTrue();
      sut.next();
      fail("it should not be possible to go through not compliant data");
    } catch (IllegalStateException e) {
      // ok
    }
  }

  private static List<FileSourceDb.Test> newFakeTests(int numberOfTests) throws IOException {
    List<FileSourceDb.Test> tests = new ArrayList<>();
    for (int i = 1; i <= numberOfTests; i++) {
      FileSourceDb.Test.Builder test = FileSourceDb.Test.newBuilder()
        .setUuid("TEST_FILE_UUID_" + i)
        .setName("NAME_" + i)
        .setStatus(TestStatus.FAILURE)
        .setStacktrace("STACKTRACE_" + i)
        .setMsg("MESSAGE_" + i)
        .setExecutionTimeMs(i);
      for (int j = 1; j <= numberOfTests; j++) {
        test.addCoveredFile(
          FileSourceDb.Test.CoveredFile.newBuilder()
            .setFileUuid("MAIN_FILE_UUID_" + j)
            .addCoveredLine(j)
          );
      }
      tests.add(test.build());
    }
    return tests;
  }

}
