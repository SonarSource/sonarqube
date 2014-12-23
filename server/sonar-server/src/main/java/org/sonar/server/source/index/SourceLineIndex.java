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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.ServerComponent;
import org.sonar.server.es.EsClient;

import java.util.List;

public class SourceLineIndex implements ServerComponent {

  private static final int MAX_RESULT = 500000;

  private final EsClient esClient;

  public SourceLineIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  /**
   * Get lines of code for file with UUID <code>fileUuid</code> with line numbers
   * between <code>from</code> and <code>to</code> (both inclusive). Line numbers
   * start at 1.
   * The max number of returned lines will be 500000.
   *
   * @param fileUuid the UUID of the file for which to get source code
   * @param from starting line; must be strictly positive
   * @param to ending line; must be greater than or equal to <code>to</code>
   */
  public List<SourceLineDoc> getLines(String fileUuid, int from, int to) {
    Preconditions.checkArgument(from > 0, "Minimum value for 'from' is 1");
    Preconditions.checkArgument(to >= from, "'to' must be larger than or equal to 'from'");
    List<SourceLineDoc> lines = Lists.newArrayList();
    int size = 1 + to - from;
    if (size > MAX_RESULT) {
      size = MAX_RESULT;
    }
    int toLimited = size + from - 1;

    for (SearchHit hit: esClient.prepareSearch(SourceLineIndexDefinition.INDEX)
      .setTypes(SourceLineIndexDefinition.TYPE)
      .setSize(size)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(SourceLineIndexDefinition.FIELD_FILE_UUID, fileUuid))
        .must(QueryBuilders.rangeQuery(SourceLineIndexDefinition.FIELD_LINE)
          .gte(from)
          .lte(toLimited)))
      .addSort(SourceLineIndexDefinition.FIELD_LINE, SortOrder.ASC)
      .get().getHits().getHits()) {
      lines.add(new SourceLineDoc(hit.sourceAsMap()));
    }

    return lines;
  }

  public long countLines(String fileUuid) {
    return esClient.prepareCount(SourceLineIndexDefinition.INDEX)
      .setTypes(SourceLineIndexDefinition.TYPE)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(SourceLineIndexDefinition.FIELD_FILE_UUID, fileUuid)))
      .get().getCount();
  }
}
