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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.fest.assertions.MapAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.test.DbTests;
import org.sonar.test.TestUtils;

import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.server.source.index.SourceLineIndexDefinition.*;

@Category(DbTests.class)
public class SourceLineIndexerTest {

  @Rule
  public EsTester es = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  @Rule
  public DbTester db = new DbTester();

  private SourceLineIndexer indexer;

  @Before
  public void setUp() {
    indexer = new SourceLineIndexer(new DbClient(db.database(), db.myBatis()), es.client());
  }

  @Test
  public void index_source_lines_from_db() throws Exception {
    db.prepareDbUnit(getClass(), "db.xml");
    indexer.index();
    assertThat(countDocuments()).isEqualTo(2);
  }

  @Test
  public void update_already_indexed_lines() throws Exception {
    prepareIndex()
      .setSource(IOUtils.toString(new FileInputStream(TestUtils.getResource(this.getClass(), "line2.json"))))
      .get();
    prepareIndex()
      .setSource(IOUtils.toString(new FileInputStream(TestUtils.getResource(this.getClass(), "line2_other_file.json"))))
      .setRefresh(true)
      .get();

    List<Integer> duplications = ImmutableList.of(1, 2, 3);
    SourceLineDoc line1 = new SourceLineDoc(ImmutableMap.<String, Object>builder()
      .put(FIELD_PROJECT_UUID, "abcd")
      .put(FIELD_FILE_UUID, "efgh")
      .put(FIELD_LINE, 1)
      .put(FIELD_SCM_REVISION, "cafebabe")
      .put(FIELD_SCM_DATE, DateUtils.parseDateTime("2014-01-01T12:34:56+0100"))
      .put(FIELD_SCM_AUTHOR, "polop")
      .put(FIELD_SOURCE, "package org.sonar.server.source;")
      .put(FIELD_DUPLICATIONS, duplications)
      .put(BaseNormalizer.UPDATED_AT_FIELD, new Date())
      .build());
    SourceLineResultSetIterator.SourceFile file = new SourceLineResultSetIterator.SourceFile("efgh", System.currentTimeMillis());
    file.addLine(line1);
    indexer.index(Iterators.singletonIterator(file));

    assertThat(countDocuments()).isEqualTo(2L);

    SearchResponse fileSearch = prepareSearch()
      .setQuery(QueryBuilders.termQuery(FIELD_FILE_UUID, "efgh"))
      .get();
    assertThat(fileSearch.getHits().getTotalHits()).isEqualTo(1L);
    Map<String, Object> fields = fileSearch.getHits().getHits()[0].sourceAsMap();
    assertThat(fields).hasSize(9);
    assertThat(fields).includes(
      MapAssert.entry(FIELD_PROJECT_UUID, "abcd"),
      MapAssert.entry(FIELD_FILE_UUID, "efgh"),
      MapAssert.entry(FIELD_LINE, 1),
      MapAssert.entry(FIELD_SCM_REVISION, "cafebabe"),
      MapAssert.entry(FIELD_SCM_DATE, "2014-01-01T11:34:56.000Z"),
      MapAssert.entry(FIELD_SCM_AUTHOR, "polop"),
      MapAssert.entry(FIELD_SOURCE, "package org.sonar.server.source;"),
      MapAssert.entry(FIELD_DUPLICATIONS, duplications)
      );
  }

  @Test
  public void delete_file_uuid() throws Exception {
    addSource("line2.json");
    addSource("line3.json");
    addSource("line2_other_file.json");

    indexer.deleteByFile("efgh");

    List<SearchHit> hits = getDocuments();
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_LINE)).isEqualTo(2);
    assertThat(document.get(FIELD_FILE_UUID)).isEqualTo("fdsq");
  }

  @Test
  public void delete_by_project_uuid() throws Exception {
    addSource("line2.json");
    addSource("line3.json");
    addSource("line2_other_file.json");
    addSource("line3_other_project.json");

    indexer.deleteByProject("abcd");

    List<SearchHit> hits = getDocuments();
    Map<String, Object> document = hits.get(0).getSource();
    assertThat(hits).hasSize(1);
    assertThat(document.get(FIELD_PROJECT_UUID)).isEqualTo("plmn");
  }

  private void addSource(String fileName) throws Exception {
    prepareIndex()
      .setSource(IOUtils.toString(new FileInputStream(TestUtils.getResource(this.getClass(), fileName))))
      .get();
  }

  private SearchRequestBuilder prepareSearch() {
    return es.client().prepareSearch(INDEX)
      .setTypes(TYPE);
  }

  private IndexRequestBuilder prepareIndex() {
    return es.client().prepareIndex(INDEX, TYPE);
  }

  private List<SearchHit> getDocuments() {
    return es.getDocuments(INDEX, TYPE);
  }

  private long countDocuments() {
    return es.countDocuments(INDEX, TYPE);
  }
}
