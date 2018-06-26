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
package org.sonar.server.view.index;

import java.util.List;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.server.es.EsClient;

import static com.google.common.collect.Lists.newArrayList;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@ServerSide
@ComputeEngineSide
public class ViewIndex {

  private static final int SCROLL_TIME_IN_MINUTES = 3;

  private final EsClient esClient;

  public ViewIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  public List<String> findAllViewUuids() {
    SearchRequestBuilder esSearch = esClient.prepareSearch(ViewIndexDefinition.INDEX_TYPE_VIEW)
      .addSort("_doc", SortOrder.ASC)
      .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES))
      .setFetchSource(false)
      .setSize(100)
      .setQuery(matchAllQuery());

    SearchResponse response = esSearch.get();
    List<String> result = newArrayList();
    while (true) {
      List<SearchHit> hits = newArrayList(response.getHits());
      for (SearchHit hit : hits) {
        result.add(hit.getId());
      }
      String scrollId = response.getScrollId();
      response = esClient.prepareSearchScroll(scrollId)
        .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES))
        .get();
      // Break condition: No hits are returned
      if (response.getHits().getHits().length == 0) {
        esClient.nativeClient().prepareClearScroll().addScrollId(scrollId).get();
        break;
      }
    }
    return result;
  }
}
