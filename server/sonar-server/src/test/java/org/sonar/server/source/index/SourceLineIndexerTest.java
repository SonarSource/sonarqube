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
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.source.index.SourceLineIndexer;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mock;

public class SourceLineIndexerTest {

  @Rule
  public EsTester es = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  private SourceLineIndexer indexer;

  @Before
  public void setUp() {
    indexer = new SourceLineIndexer(mock(DbClient.class), es.client());
  }

  @Test
  public void should_index_source_lines() {
    BulkIndexer bulk = new BulkIndexer(es.client(), SourceLineIndexDefinition.INDEX_SOURCE_LINES);

    SourceLineDoc line1 = new SourceLineDoc(ImmutableMap.<String, Object>builder()
      .put(SourceLineIndexDefinition.FIELD_PROJECT_UUID, "abcd")
      .put(SourceLineIndexDefinition.FIELD_FILE_UUID, "efgh")
      .put(SourceLineIndexDefinition.FIELD_LINE, 42)
      .put(SourceLineIndexDefinition.FIELD_SCM_REVISION, "cafebabe")
      .put(SourceLineIndexDefinition.FIELD_SCM_DATE, "2014-01-01T12:34:56.7+0100")
      .put(SourceLineIndexDefinition.FIELD_SCM_AUTHOR, "polop")
      .put(SourceLineIndexDefinition.FIELD_SOURCE, "package org.sonar.server.source;")
      .put(BaseNormalizer.UPDATED_AT_FIELD, new Date())
      .build());
    Collection<SourceLineDoc> sourceLines = ImmutableList.of(line1);

    List<Collection<SourceLineDoc>> sourceLineContainer = Lists.newArrayList();
    sourceLineContainer.add(sourceLines);
    indexer.indexSourceLines(bulk, sourceLineContainer.iterator());
  }
}
