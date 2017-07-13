/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.test.index;

import com.google.common.collect.Iterators;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.source.index.FileSourcesUpdaterHelper;
import org.sonar.server.test.db.TestTesting;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.server.es.DefaultIndexSettings.REFRESH_IMMEDIATE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_DURATION_IN_MS;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_MESSAGE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_NAME;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_PROJECT_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_STACKTRACE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_STATUS;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_TEST_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.INDEX_TYPE_TEST;

public class TestIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester es = new EsTester(new TestIndexDefinition(new MapSettings().asConfig()));

  @Rule
  public DbTester db = DbTester.create(system2);

  private TestIndexer underTest = new TestIndexer(db.getDbClient(), es.client());

  @Test
  public void index_on_startup() {
    TestIndexer indexer = spy(underTest);
    doNothing().when(indexer).indexOnStartup(null);
    indexer.indexOnStartup(null);
    verify(indexer).indexOnStartup(null);
  }

  @Test
  public void index_tests() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");
    TestTesting.updateDataColumn(db.getSession(), "FILE_UUID", TestTesting.newRandomTests(3));

    underTest.indexOnStartup(null);

    assertThat(countDocuments()).isEqualTo(3);
  }

  @Test
  public void index_tests_from_project() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");

    TestTesting.updateDataColumn(db.getSession(), "FILE_UUID", TestTesting.newRandomTests(3));

    underTest.indexProject("PROJECT_UUID", ProjectIndexer.Cause.NEW_ANALYSIS);
    assertThat(countDocuments()).isEqualTo(3);
  }

  @Test
  public void index_nothing_from_unknown_project() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");

    TestTesting.updateDataColumn(db.getSession(), "FILE_UUID", TestTesting.newRandomTests(3));

    underTest.indexProject("UNKNOWN", ProjectIndexer.Cause.NEW_ANALYSIS);
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
      DbFileSources.Test.newBuilder()
        .setUuid("U111")
        .setName("NAME_1")
        .setStatus(DbFileSources.Test.TestStatus.FAILURE)
        .setMsg("NEW_MESSAGE_1")
        .setStacktrace("NEW_STACKTRACE_1")
        .setExecutionTimeMs(123_456L)
        .addCoveredFile(DbFileSources.Test.CoveredFile.newBuilder().setFileUuid("MAIN_UUID_1").addCoveredLine(42))
        .build()));
    underTest.index(Iterators.singletonIterator(dbRow));

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
      entry(FIELD_DURATION_IN_MS, 123_456));
  }

  @Test
  public void delete_file_by_uuid() throws Exception {
    indexTest("P1", "F1", "T1", "U111");
    indexTest("P1", "F1", "T2", "U112");
    indexTest("P1", "F2", "T1", "U121");

    underTest.deleteByFile("F1");

    List<SearchHit> hits = getDocuments();
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_NAME)).isEqualTo("NAME_1");
    assertThat(document.get(FIELD_FILE_UUID)).isEqualTo("F2");
  }

  @Test
  public void delete_project_by_uuid() throws Exception {
    indexTest("P1", "F1", "T1", "U111");
    indexTest("P1", "F1", "T2", "U112");
    indexTest("P1", "F2", "T1", "U121");
    indexTest("P2", "F3", "T1", "U231");

    underTest.deleteProject("P1");

    List<SearchHit> hits = getDocuments();
    assertThat(hits).hasSize(1);
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_PROJECT_UUID)).isEqualTo("P2");
  }

  private void indexTest(String projectUuid, String fileUuid, String testName, String uuid) throws IOException {
    es.client().prepareIndex(INDEX_TYPE_TEST)
      .setId(uuid)
      .setRouting(projectUuid)
      .setSource(IOUtils.toString(getClass().getResource(format("%s/%s_%s_%s.json", getClass().getSimpleName(), projectUuid, fileUuid, testName))))
      .setRefreshPolicy(REFRESH_IMMEDIATE)
      .get();
  }

  private SearchRequestBuilder prepareSearch() {
    return es.client().prepareSearch(INDEX_TYPE_TEST);
  }

  private List<SearchHit> getDocuments() {
    return es.getDocuments(INDEX_TYPE_TEST);
  }

  private long countDocuments() {
    return es.countDocuments(INDEX_TYPE_TEST);
  }
}
