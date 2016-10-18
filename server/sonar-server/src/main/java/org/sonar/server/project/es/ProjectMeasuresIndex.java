/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.project.es;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.server.component.ws.SearchProjectsQueryBuilder.SearchProjectsCriteriaQuery;
import org.sonar.server.component.ws.SearchProjectsQueryBuilder.SearchProjectsCriteriaQuery.MetricCriteria;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.FIELD_MEASURES;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.FIELD_MEASURES_KEY;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.FIELD_MEASURES_VALUE;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndex extends BaseIndex {

  private static final String FIELD_KEY = FIELD_MEASURES + "." + FIELD_MEASURES_KEY;
  private static final String FIELD_VALUE = FIELD_MEASURES + "." + FIELD_MEASURES_VALUE;

  public ProjectMeasuresIndex(EsClient client) {
    super(client);
  }

  public SearchIdResult<String> search(SearchProjectsCriteriaQuery query, SearchOptions searchOptions) {
    BoolQueryBuilder metricFilters = boolQuery();
    query.getMetricCriterias().stream()
      .map(criteria -> nestedQuery(FIELD_MEASURES, boolQuery()
        .filter(termQuery(FIELD_KEY, criteria.getMetricKey()))
        .filter(toValueQuery(criteria))))
      .forEach(metricFilters::filter);
    QueryBuilder esQuery = query.getMetricCriterias().isEmpty() ? matchAllQuery() : metricFilters;

    SearchRequestBuilder request = getClient()
      .prepareSearch(INDEX_PROJECT_MEASURES)
      .setTypes(TYPE_PROJECT_MEASURES)
      .setFetchSource(false)
      .setQuery(esQuery)
      .setFrom(searchOptions.getOffset())
      .setSize(searchOptions.getLimit())
      .addSort(FIELD_NAME + "." + SORT_SUFFIX, SortOrder.ASC);

    return new SearchIdResult<>(request.get(), id -> id);
  }

  private static QueryBuilder toValueQuery(MetricCriteria criteria) {
    String fieldName = FIELD_VALUE;

    switch (criteria.getOperator()) {
      case EQ:
        return termQuery(fieldName, criteria.getValue());
      case GT:
        return rangeQuery(fieldName).gt(criteria.getValue());
      case LTE:
        return rangeQuery(fieldName).lte(criteria.getValue());
      default:
        throw new IllegalStateException("Metric criteria non supported: " + criteria.getOperator().name());
    }

  }
}
