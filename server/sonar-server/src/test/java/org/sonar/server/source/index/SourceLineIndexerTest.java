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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.fest.assertions.MapAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.test.TestUtils;

import java.io.FileInputStream;
import java.util.Date;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class SourceLineIndexerTest {

  @Rule
  public EsTester es = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  @Rule
  public TestDatabase db = new TestDatabase();

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
    es.client().prepareIndex(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE)
      .setSource(IOUtils.toString(new FileInputStream(TestUtils.getResource(this.getClass(), "line2.json"))))
      .get();
    es.client().prepareIndex(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE)
      .setSource(IOUtils.toString(new FileInputStream(TestUtils.getResource(this.getClass(), "line2_other_file.json"))))
      .setRefresh(true)
      .get();

    SourceLineDoc line1 = new SourceLineDoc(ImmutableMap.<String, Object>builder()
      .put(SourceLineIndexDefinition.FIELD_PROJECT_UUID, "abcd")
      .put(SourceLineIndexDefinition.FIELD_FILE_UUID, "efgh")
      .put(SourceLineIndexDefinition.FIELD_LINE, 1)
      .put(SourceLineIndexDefinition.FIELD_SCM_REVISION, "cafebabe")
      .put(SourceLineIndexDefinition.FIELD_SCM_DATE, DateUtils.parseDateTime("2014-01-01T12:34:56+0100"))
      .put(SourceLineIndexDefinition.FIELD_SCM_AUTHOR, "polop")
      .put(SourceLineIndexDefinition.FIELD_SOURCE, "package org.sonar.server.source;")
      .put(BaseNormalizer.UPDATED_AT_FIELD, new Date())
      .build());
    SourceLineResultSetIterator.SourceFile file = new SourceLineResultSetIterator.SourceFile("efgh", System.currentTimeMillis());
    file.addLine(line1);
    indexer.index(Iterators.singletonIterator(file));

    assertThat(countDocuments()).isEqualTo(2L);

    SearchResponse fileSearch = es.client().prepareSearch(SourceLineIndexDefinition.INDEX)
      .setTypes(SourceLineIndexDefinition.TYPE)
      .setQuery(QueryBuilders.termQuery(SourceLineIndexDefinition.FIELD_FILE_UUID, "efgh"))
      .get();
    assertThat(fileSearch.getHits().getTotalHits()).isEqualTo(1L);
    Map<String, Object> fields = fileSearch.getHits().getHits()[0].sourceAsMap();
    assertThat(fields).hasSize(8);
    assertThat(fields).includes(
      MapAssert.entry(SourceLineIndexDefinition.FIELD_PROJECT_UUID, "abcd"),
      MapAssert.entry(SourceLineIndexDefinition.FIELD_FILE_UUID, "efgh"),
      MapAssert.entry(SourceLineIndexDefinition.FIELD_LINE, 1),
      MapAssert.entry(SourceLineIndexDefinition.FIELD_SCM_REVISION, "cafebabe"),
      MapAssert.entry(SourceLineIndexDefinition.FIELD_SCM_DATE, "2014-01-01T11:34:56.000Z"),
      MapAssert.entry(SourceLineIndexDefinition.FIELD_SCM_AUTHOR, "polop"),
      MapAssert.entry(SourceLineIndexDefinition.FIELD_SOURCE, "package org.sonar.server.source;")
      );
  }

  private long countDocuments() {
    return es.countDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE);
  }
}
