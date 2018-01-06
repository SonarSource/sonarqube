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
package org.sonar.server.test.index;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.es.EsTester;
import org.sonar.server.test.db.TestTesting;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.server.es.DefaultIndexSettings.REFRESH_IMMEDIATE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_MESSAGE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_NAME;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_STACKTRACE;
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

    underTest.indexOnAnalysis("PROJECT_UUID");
    assertThat(countDocuments()).isEqualTo(3);
  }

  @Test
  public void index_nothing_from_unknown_project() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");

    TestTesting.updateDataColumn(db.getSession(), "FILE_UUID", TestTesting.newRandomTests(3));

    underTest.indexOnAnalysis("UNKNOWN");
    assertThat(countDocuments()).isZero();
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
  public void long_message_can_be_indexed() throws Exception {
    indexTest("P3", "F1", "long_message", "U111");

    assertThat(countDocuments()).isEqualTo(1);

    List<SearchHit> hits = getDocuments();
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_MESSAGE).toString()).hasSize(50000);
    assertThat(document.get(FIELD_FILE_UUID)).isEqualTo("F1");
  }

  @Test
  public void long_stacktrace_can_be_indexed() throws Exception {
    indexTest("P3", "F1", "long_stacktrace", "U111");

    assertThat(countDocuments()).isEqualTo(1);

    List<SearchHit> hits = getDocuments();
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_STACKTRACE).toString()).hasSize(50000);
    assertThat(document.get(FIELD_FILE_UUID)).isEqualTo("F1");
  }

  @Test
  public void long_name_can_be_indexed() throws Exception {
    indexTest("P3", "F1", "long_name", "U111");

    assertThat(countDocuments()).isEqualTo(1);

    List<SearchHit> hits = getDocuments();
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_NAME).toString()).hasSize(50000);
    assertThat(document.get(FIELD_FILE_UUID)).isEqualTo("F1");
  }

  private void indexTest(String projectUuid, String fileUuid, String testName, String uuid) throws IOException {
    String json = IOUtils.toString(getClass().getResource(format("%s/%s_%s_%s.json", getClass().getSimpleName(), projectUuid, fileUuid, testName)));
    es.client().prepareIndex(INDEX_TYPE_TEST)
      .setId(uuid)
      .setRouting(projectUuid)
      .setSource(json, XContentType.JSON)
      .setRefreshPolicy(REFRESH_IMMEDIATE)
      .get();
  }

  private List<SearchHit> getDocuments() {
    return es.getDocuments(INDEX_TYPE_TEST);
  }

  private long countDocuments() {
    return es.countDocuments(INDEX_TYPE_TEST);
  }
}
