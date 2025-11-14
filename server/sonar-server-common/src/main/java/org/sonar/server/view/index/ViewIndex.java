/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.server.es.EsClient;

import static com.google.common.collect.Lists.newArrayList;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * The View Index indexes all views, root and not root (APP, VW, SVW), and all projects that are computed under each view.
 * It's based on the computation results, coming from the components table, not on the definition of those views.
 */
@ServerSide
@ComputeEngineSide
public class ViewIndex {

  private static final int SCROLL_TIME_IN_MINUTES = 3;

  private final EsClient esClient;

  public ViewIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  public List<String> findAllViewUuids() {
    SearchRequest esSearch = EsClient.prepareSearch(ViewIndexDefinition.TYPE_VIEW)
      .source(new SearchSourceBuilder()
        .sort("_doc", SortOrder.ASC)
        .fetchSource(false)
        .size(100)
        .query(matchAllQuery()))
      .scroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES));

    SearchResponse response = esClient.search(esSearch);
    List<String> result = new ArrayList<>();
    while (true) {
      List<SearchHit> hits = newArrayList(response.getHits());
      for (SearchHit hit : hits) {
        result.add(hit.getId());
      }
      String scrollId = response.getScrollId();
      response = esClient.scroll(new SearchScrollRequest().scrollId(scrollId).scroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES)));
      // Break condition: No hits are returned
      if (response.getHits().getHits().length == 0) {
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        esClient.clearScroll(clearScrollRequest);
        break;
      }
    }
    return result;
  }
}
