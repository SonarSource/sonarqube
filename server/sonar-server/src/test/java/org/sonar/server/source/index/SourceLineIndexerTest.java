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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.test.DbTests;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_DUPLICATIONS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_IT_CONDITIONS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_IT_COVERED_CONDITIONS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_IT_LINE_HITS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_LINE;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_OVERALL_CONDITIONS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_OVERALL_COVERED_CONDITIONS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_OVERALL_LINE_HITS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_PROJECT_UUID;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_SCM_AUTHOR;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_SCM_REVISION;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_SOURCE;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_UT_CONDITIONS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_UT_COVERED_CONDITIONS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.FIELD_UT_LINE_HITS;
import static org.sonar.server.source.index.SourceLineIndexDefinition.INDEX;
import static org.sonar.server.source.index.SourceLineIndexDefinition.TYPE;

@Category(DbTests.class)
public class SourceLineIndexerTest {

  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private SourceLineIndexer indexer;

  @Before
  public void setUp() {
    es.truncateIndices();
    indexer = new SourceLineIndexer(new DbClient(db.database(), db.myBatis()), es.client());
    indexer.setEnabled(true);
  }

  @Test
  public void index_source_lines() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");

    try (Connection connection = db.openConnection()) {
      FileSourceTesting.updateDataColumn(connection, "FILE_UUID", FileSourceTesting.newRandomData(3).build());
    }

    indexer.index();
    assertThat(countDocuments()).isEqualTo(3);
  }

  @Test
  public void index_source_lines_from_project() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");

    try (Connection connection = db.openConnection()) {
      FileSourceTesting.updateDataColumn(connection, "FILE_UUID", FileSourceTesting.newRandomData(3).build());
    }

    indexer.index("PROJECT_UUID");
    assertThat(countDocuments()).isEqualTo(3);
  }

  @Test
  public void index_nothing_from_unknown_project() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");

    try (Connection connection = db.openConnection()) {
      FileSourceTesting.updateDataColumn(connection, "FILE_UUID", FileSourceTesting.newRandomData(3).build());
    }

    indexer.index("UNKNOWN");
    assertThat(countDocuments()).isZero();
  }

  /**
   * File F1 in project P1 has one line -> to be updated
   * File F2 in project P1 has one line -> untouched
   */
  @Test
  public void update_already_indexed_lines() throws Exception {
    indexLine("P1", "F1", 1);
    indexLine("P1", "F2", 1);

    List<Integer> duplications = ImmutableList.of(1, 2, 3);
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    dataBuilder.addLinesBuilder()
      .setLine(1)
      .setScmRevision("new_revision")
      .setScmAuthor("new_author")
      .setSource("new source")
      .addAllDuplication(duplications)
      .build();
    FileSourcesUpdaterHelper.Row dbRow = SourceLineResultSetIterator.toRow("P1", "F1", new Date(), dataBuilder.build());
    indexer.index(Iterators.singletonIterator(dbRow));

    assertThat(countDocuments()).isEqualTo(2L);

    SearchResponse fileSearch = prepareSearch()
      .setQuery(QueryBuilders.termQuery(FIELD_FILE_UUID, "F1"))
      .get();
    assertThat(fileSearch.getHits().getTotalHits()).isEqualTo(1L);
    Map<String, Object> fields = fileSearch.getHits().getHits()[0].sourceAsMap();
    assertThat(fields).contains(
      entry(FIELD_PROJECT_UUID, "P1"),
      entry(FIELD_FILE_UUID, "F1"),
      entry(FIELD_LINE, 1),
      entry(FIELD_SCM_REVISION, "new_revision"),
      entry(FIELD_SCM_AUTHOR, "new_author"),
      entry(FIELD_SOURCE, "new source"),
      entry(FIELD_DUPLICATIONS, duplications));
  }

  @Test
  public void delete_file_uuid() throws Exception {
    indexLine("P1", "F1", 1);
    indexLine("P1", "F1", 2);
    indexLine("P1", "F2", 1);

    indexer.deleteByFile("F1");

    List<SearchHit> hits = getDocuments();
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_LINE)).isEqualTo(1);
    assertThat(document.get(FIELD_FILE_UUID)).isEqualTo("F2");
  }

  @Test
  public void delete_by_project_uuid() throws Exception {
    indexLine("P1", "F1", 1);
    indexLine("P1", "F1", 2);
    indexLine("P1", "F2", 1);
    indexLine("P2", "F3", 1);

    indexer.deleteByProject("P1");

    List<SearchHit> hits = getDocuments();
    assertThat(hits).hasSize(1);
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_PROJECT_UUID)).isEqualTo("P2");
  }

  @Test
  public void index_source_lines_with_big_test_data() {
    Integer bigValue = Short.MAX_VALUE * 2;

    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    dataBuilder.addLinesBuilder()
      .setLine(1)
      .setScmRevision("cafebabe")
      .setScmAuthor("polop")
      .setScmDate(DateUtils.parseDateTime("2014-01-01T12:34:56+0100").getTime())
      .setSource("package org.sonar.server.source;")
      .setUtLineHits(bigValue)
      .setUtConditions(bigValue)
      .setUtCoveredConditions(bigValue)
      .setItLineHits(bigValue)
      .setItConditions(bigValue)
      .setItCoveredConditions(bigValue)
      .setOverallLineHits(bigValue)
      .setOverallConditions(bigValue)
      .setOverallCoveredConditions(bigValue)
      .build();

    FileSourcesUpdaterHelper.Row row = SourceLineResultSetIterator.toRow("P1", "F1", new Date(), dataBuilder.build());
    indexer.index(Iterators.singletonIterator(row));

    List<SearchHit> hits = getDocuments();
    assertThat(hits).hasSize(1);
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(document.get(FIELD_UT_LINE_HITS)).isEqualTo(bigValue);
    assertThat(document.get(FIELD_UT_CONDITIONS)).isEqualTo(bigValue);
    assertThat(document.get(FIELD_UT_COVERED_CONDITIONS)).isEqualTo(bigValue);
    assertThat(document.get(FIELD_IT_LINE_HITS)).isEqualTo(bigValue);
    assertThat(document.get(FIELD_IT_CONDITIONS)).isEqualTo(bigValue);
    assertThat(document.get(FIELD_IT_COVERED_CONDITIONS)).isEqualTo(bigValue);
    assertThat(document.get(FIELD_OVERALL_LINE_HITS)).isEqualTo(bigValue);
    assertThat(document.get(FIELD_OVERALL_CONDITIONS)).isEqualTo(bigValue);
    assertThat(document.get(FIELD_OVERALL_COVERED_CONDITIONS)).isEqualTo(bigValue);
  }

  private void indexLine(String projectUuid, String fileUuid, int line) throws IOException {
    es.client().prepareIndex(INDEX, TYPE)
      .setId(SourceLineIndexDefinition.docKey(fileUuid, line))
      .setSource(FileUtils.readFileToString(TestUtils.getResource(this.getClass(), projectUuid + "_" + fileUuid + "_line" + line + ".json")))
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
