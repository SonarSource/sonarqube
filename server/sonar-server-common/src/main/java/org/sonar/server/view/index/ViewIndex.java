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

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.server.es.EsClient;

/**
 * The View Index indexes all views, root and not root (APP, VW, SVW), and all projects that are computed under each view.
 * It's based on the computation results, coming from the components table, not on the definition of those views.
 */
@ServerSide
@ComputeEngineSide
public class ViewIndex {

  private static final String SCROLL_TIMEOUT_IN_MINUTES = "3m";

  private final EsClient esClient;

  public ViewIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  public List<String> findAllViewUuids() {
    SearchResponse<Void> searchResponse = esClient.searchV2(req -> req
      .index(ViewIndexDefinition.TYPE_VIEW.getMainType().getIndex().getName())
      .scroll(t -> t.time(SCROLL_TIMEOUT_IN_MINUTES))
      .sort(s -> s.doc(d -> d.order(SortOrder.Asc)))
      .source(s -> s.fetch(false))
      .size(100)
      .query(q -> q.matchAll(m -> m)),
      Void.class);

    //process initial search results
    List<String> result = new ArrayList<>(searchResponse.hits().hits()
      .stream()
      .map(Hit::id)
      .toList());

    String scrollId = searchResponse.scrollId();
    if (scrollId == null) {
      return result;
    }

    // Continue with scroll
    while (scrollId != null) {
      final String currentScrollId = scrollId;
      ScrollResponse<Void> scrollResponse = esClient.scrollV2(ssr ->
        ssr.scrollId(currentScrollId)
          .scroll(b -> b.time(SCROLL_TIMEOUT_IN_MINUTES)));

      List<Hit<Void>> hits = scrollResponse.hits().hits();

      // Break condition: No hits are returned
      if (hits.isEmpty()) {
        esClient.clearScrollV2(csr -> csr.scrollId(currentScrollId));
        break;
      }

      // Process scroll results
      for (Hit<Void> hit : hits) {
        result.add(hit.id());
      }

      scrollId = scrollResponse.scrollId();
    }

    return result;
  }
}
