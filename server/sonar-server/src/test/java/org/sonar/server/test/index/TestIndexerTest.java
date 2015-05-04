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

import com.google.common.collect.Iterators;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.db.FileSourceDb.Test.TestStatus;
import org.sonar.server.source.index.FileSourcesUpdaterHelper;
import org.sonar.server.test.db.TestTesting;
import org.sonar.test.DbTests;
import org.sonar.test.TestUtils;

import java.io.IOException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_DURATION_IN_MS;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_MESSAGE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_NAME;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_PROJECT_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_STACKTRACE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_STATUS;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_TEST_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.INDEX;
import static org.sonar.server.test.index.TestIndexDefinition.TYPE;

@Category(DbTests.class)
public class TestIndexerTest {

  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new TestIndexDefinition(new Settings()));

  @ClassRule
  public static DbTester db = new DbTester();

  private TestIndexer sut;

  @Before
  public void setUp() {
    es.truncateIndices();
    db.truncateTables();
    sut = new TestIndexer(new DbClient(db.database(), db.myBatis()), es.client());
    sut.setEnabled(true);
  }

  @Test
  public void index_tests() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");
    Connection connection = db.openConnection();
    TestTesting.updateDataColumn(connection, "FILE_UUID", TestTesting.newRandomTests(3));
    connection.close();

    sut.index();

    assertThat(countDocuments()).isEqualTo(3);
  }

  @Test
  public void index_tests_from_project() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");

    Connection connection = db.openConnection();
    TestTesting.updateDataColumn(connection, "FILE_UUID", TestTesting.newRandomTests(3));
    connection.close();

    sut.index("PROJECT_UUID");
    assertThat(countDocuments()).isEqualTo(3);
  }

  @Test
  public void index_nothing_from_unknown_project() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");

    Connection connection = db.openConnection();
    TestTesting.updateDataColumn(connection, "FILE_UUID", TestTesting.newRandomTests(3));
    connection.close();

    sut.index("UNKNOWN");
    assertThat(countDocuments()).isZero();
  }

  /**
   * File F1 in project P1 has one test -> to be updated
   * File F2 in project P1 has one test -> untouched
   */

  @Test
  public void update_already_indexed_test() throws Exception {
    indexTest("P1", "F1", "T1", "U111");
    indexTest("P1", "F2", "T1", "U121");

    FileSourcesUpdaterHelper.Row dbRow = TestResultSetIterator.toRow("P1", "F1", new Date(), Arrays.asList(
      FileSourceDb.Test.newBuilder()
        .setUuid("U111")
        .setName("NAME_1")
        .setStatus(TestStatus.FAILURE)
        .setMsg("NEW_MESSAGE_1")
        .setStacktrace("NEW_STACKTRACE_1")
        .setExecutionTimeMs(123_456L)
        .addCoveredFile(FileSourceDb.Test.CoveredFile.newBuilder().setFileUuid("MAIN_UUID_1").addCoveredLine(42))
        .build()
      ));
    sut.index(Iterators.singletonIterator(dbRow));

    assertThat(countDocuments()).isEqualTo(2L);

    SearchResponse fileSearch = prepareSearch()
      .setQuery(QueryBuilders.termQuery(FIELD_FILE_UUID, "F1"))
      .get();
    assertThat(fileSearch.getHits().getTotalHits()).isEqualTo(1L);
    Map<String, Object> fields = fileSearch.getHits().getHits()[0].sourceAsMap();
    assertThat(fields).contains(
      entry(FIELD_PROJECT_UUID, "P1"),
      entry(FIELD_FILE_UUID, "F1"),
      entry(FIELD_TEST_UUID, "U111"),
      entry(FIELD_NAME, "NAME_1"),
      entry(FIELD_STATUS, "FAILURE"),
      entry(FIELD_MESSAGE, "NEW_MESSAGE_1"),
      entry(FIELD_STACKTRACE, "NEW_STACKTRACE_1"),
      entry(FIELD_DURATION_IN_MS, 123_456)
      );
  }

  @Test
  public void delete_file_uuid() throws Exception {
    indexTest("P1", "F1", "T1", "U111");
    indexTest("P1", "F1", "T2", "U112");
    indexTest("P1", "F2", "T1", "U121");

    sut.deleteByFile("F1");

    List<SearchHit> hits = getDocuments();
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_NAME)).isEqualTo("NAME_1");
    assertThat(document.get(FIELD_FILE_UUID)).isEqualTo("F2");
  }

  @Test
  public void delete_by_project_uuid() throws Exception {
    indexTest("P1", "F1", "T1", "U111");
    indexTest("P1", "F1", "T2", "U112");
    indexTest("P1", "F2", "T1", "U121");
    indexTest("P2", "F3", "T1", "U231");

    sut.deleteByProject("P1");

    List<SearchHit> hits = getDocuments();
    assertThat(hits).hasSize(1);
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_PROJECT_UUID)).isEqualTo("P2");
  }

  private void indexTest(String projectUuid, String fileUuid, String testName, String uuid) throws IOException {
    es.client().prepareIndex(INDEX, TYPE)
      .setId(uuid)
      .setSource(FileUtils.readFileToString(TestUtils.getResource(this.getClass(), projectUuid + "_" + fileUuid + "_" + testName + ".json")))
      .setRefresh(true)
      .get();
  }

  private SearchRequestBuilder prepareSearch() {
    return es.client().prepareSearch(INDEX)
      .setTypes(TYPE);
  }

  private List<SearchHit> getDocuments() {
    return es.getDocuments(INDEX, TYPE);
  }

  private long countDocuments() {
    return es.countDocuments(INDEX, TYPE);
  }
}
