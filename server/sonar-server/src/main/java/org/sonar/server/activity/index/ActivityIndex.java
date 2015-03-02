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
package org.sonar.server.activity.index;

import com.google.common.base.Function;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;

import java.util.Date;
import java.util.Map;

public class ActivityIndex extends BaseIndex {

  /**
   * Convert an Elasticsearch result (a map) to an {@link org.sonar.server.activity.index.ActivityDoc}. It's
   * used for {@link org.sonar.server.es.SearchResult}.
   */
  private static final Function<Map<String, Object>, ActivityDoc> DOC_CONVERTER = new NonNullInputFunction<Map<String, Object>, ActivityDoc>() {
    @Override
    protected ActivityDoc doApply(Map<String, Object> input) {
      return new ActivityDoc(input);
    }
  };

  public ActivityIndex(EsClient esClient) {
    super(esClient);
  }

  public SearchResult<ActivityDoc> search(ActivityQuery query, SearchOptions options) {
    SearchResponse response = doSearch(query, options);
    return new SearchResult<>(response, DOC_CONVERTER);
  }

  public SearchResponse doSearch(ActivityQuery query, SearchOptions options) {
    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(ActivityIndexDefinition.INDEX)
      .setTypes(ActivityIndexDefinition.TYPE);

    requestBuilder.setFrom(options.getOffset());
    requestBuilder.setSize(options.getLimit());
    requestBuilder.addSort(ActivityIndexDefinition.FIELD_CREATED_AT, SortOrder.DESC);

    AndFilterBuilder filter = FilterBuilders.andFilter();
    if (!query.getTypes().isEmpty()) {
      OrFilterBuilder typeFilter = FilterBuilders.orFilter();
      for (String type : query.getTypes()) {
        typeFilter.add(FilterBuilders.termFilter(ActivityIndexDefinition.FIELD_TYPE, type));
      }
      filter.add(typeFilter);
    }

    if (!query.getDataOrFilters().isEmpty()) {
      for (Map.Entry<String, Object> entry : query.getDataOrFilters().entrySet()) {
        OrFilterBuilder orFilter = FilterBuilders.orFilter();
        orFilter.add(FilterBuilders.nestedFilter(ActivityIndexDefinition.FIELD_DETAILS,
          FilterBuilders.termFilter(ActivityIndexDefinition.FIELD_DETAILS + "." + entry.getKey(), entry.getValue())));
        filter.add(orFilter);
      }
    }

    Date since = query.getSince();
    if (since != null) {
      filter.add(FilterBuilders.rangeFilter(ActivityIndexDefinition.FIELD_CREATED_AT)
        .gt(since)
        .cache(false));
    }
    Date to = query.getTo();
    if (to != null) {
      filter.add(FilterBuilders.rangeFilter(ActivityIndexDefinition.FIELD_CREATED_AT)
        .lt(to)
        .cache(false));
    }

    requestBuilder.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filter));
    return requestBuilder.get();
  }
}
