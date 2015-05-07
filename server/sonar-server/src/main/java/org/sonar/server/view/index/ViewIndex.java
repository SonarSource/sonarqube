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

package org.sonar.server.view.index;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.ServerSide;
import org.sonar.server.es.EsClient;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class ViewIndex {

  private static final int SCROLL_TIME_IN_MINUTES = 3;

  private final EsClient esClient;

  public ViewIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  public List<String> findAllViewUuids() {
    SearchRequestBuilder esSearch = esClient.prepareSearch(ViewIndexDefinition.INDEX)
      .setTypes(ViewIndexDefinition.TYPE_VIEW)
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES))
      .setFetchSource(ViewIndexDefinition.FIELD_UUID, null)
      .setSize(100)
      .setQuery(QueryBuilders.matchAllQuery());

    SearchResponse response = esSearch.get();
    List<String> result = newArrayList();
    while (true) {
      List<SearchHit> hits = newArrayList(response.getHits());
      for (SearchHit hit : hits) {
        result.add((String) hit.getSource().get(ViewIndexDefinition.FIELD_UUID));
      }
      response = esClient.prepareSearchScroll(response.getScrollId())
        .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES))
        .get();
      // Break condition: No hits are returned
      if (response.getHits().getHits().length == 0) {
        break;
      }
    }
    return result;
  }

  public void delete(Collection<String> viewUuids) {
    esClient
      .prepareDeleteByQuery(ViewIndexDefinition.INDEX)
      .setTypes(ViewIndexDefinition.TYPE_VIEW)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
        FilterBuilders.termsFilter(ViewIndexDefinition.FIELD_UUID, viewUuids)
        ))
      .get();
  }
}
