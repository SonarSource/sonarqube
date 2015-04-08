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

import com.google.common.collect.Lists;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class SourceLineIndex extends BaseIndex {

  private static final int MAX_RESULT = 500000;

  public SourceLineIndex(EsClient esClient) {
    super(esClient);
  }

  /**
   * Get lines of code for file with UUID <code>fileUuid</code> with line numbers
   * between <code>from</code> and <code>to</code> (both inclusive). Line numbers
   * start at 1.
   * The max number of returned lines will be 500_000.
   *
   * @param fileUuid the UUID of the file for which to get source code
   * @param from starting line; must be strictly positive
   * @param to ending line; must be greater than or equal to <code>to</code>
   */
  public List<SourceLineDoc> getLines(String fileUuid, int from, int to) {
    checkArgument(from > 0, "Minimum value for 'from' is 1");
    checkArgument(to >= from, "'to' must be larger than or equal to 'from'");
    List<SourceLineDoc> lines = Lists.newArrayList();
    int size = 1 + to - from;
    if (size > MAX_RESULT) {
      size = MAX_RESULT;
    }
    int toLimited = size + from - 1;

    for (SearchHit hit : getClient().prepareSearch(SourceLineIndexDefinition.INDEX)
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

  /**
   * Get lines of code for file with UUID <code>fileUuid</code>.
   */
  public List<SourceLineDoc> getLines(String fileUuid) {
    List<SourceLineDoc> lines = Lists.newArrayList();

    for (SearchHit hit : getClient().prepareSearch(SourceLineIndexDefinition.INDEX)
      .setTypes(SourceLineIndexDefinition.TYPE)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(SourceLineIndexDefinition.FIELD_FILE_UUID, fileUuid)))
      .addSort(SourceLineIndexDefinition.FIELD_LINE, SortOrder.ASC)
      .get().getHits().getHits()) {
      lines.add(new SourceLineDoc(hit.sourceAsMap()));
    }

    return lines;
  }

  public SourceLineDoc getLine(String fileUuid, int line) {
    checkArgument(line > 0, "Line should be greater than 0");
    SearchRequestBuilder request = getClient().prepareSearch(SourceLineIndexDefinition.INDEX)
      .setTypes(SourceLineIndexDefinition.TYPE)
      .setSize(1)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(SourceLineIndexDefinition.FIELD_FILE_UUID, fileUuid))
        .must(QueryBuilders.rangeQuery(SourceLineIndexDefinition.FIELD_LINE)
          .gte(line)
          .lte(line)))
      .addSort(SourceLineIndexDefinition.FIELD_LINE, SortOrder.ASC);

    SearchHit[] result = request.get().getHits().getHits();
    if (result.length == 1) {
      return new SourceLineDoc(result[0].sourceAsMap());
    }
    throw new NotFoundException(String.format("No source found on line %s for file '%s'", line, fileUuid));
  }

  @CheckForNull
  public Date lastCommitDateOnProject(String projectUuid) {
    SearchRequestBuilder request = getClient().prepareSearch(SourceLineIndexDefinition.INDEX)
      .setTypes(SourceLineIndexDefinition.TYPE)
      .setSize(1)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(SourceLineIndexDefinition.FIELD_PROJECT_UUID, projectUuid)))
      .setFetchSource(new String[]{SourceLineIndexDefinition.FIELD_SCM_DATE}, null)
      .addSort(SourceLineIndexDefinition.FIELD_SCM_DATE, SortOrder.DESC);

    SearchHit[] result = request.get().getHits().getHits();
    if (result.length > 0) {
      return new SourceLineDoc(result[0].sourceAsMap()).scmDate();
    }

    return null;
  }
}
