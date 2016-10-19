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
package org.sonar.server.component.es;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.Set;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.bucket.range.RangeBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.server.component.es.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.user.UserSession;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filters;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.FIELD_MEASURES;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.FIELD_MEASURES_KEY;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.FIELD_MEASURES_VALUE;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.FIELD_QUALITY_GATE;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.*;

public class ProjectMeasuresIndex extends BaseIndex {

  private static final String FIELD_KEY = FIELD_MEASURES + "." + FIELD_MEASURES_KEY;
  private static final String FIELD_VALUE = FIELD_MEASURES + "." + FIELD_MEASURES_VALUE;

  private final UserSession userSession;

  public ProjectMeasuresIndex(EsClient client, UserSession userSession) {
    super(client);
    this.userSession = userSession;
  }

  public SearchIdResult<String> search(ProjectMeasuresQuery query, SearchOptions searchOptions) {
    QueryBuilder esQuery = createEsQuery(query);

    SearchRequestBuilder request = getClient()
      .prepareSearch(INDEX_PROJECT_MEASURES)
      .setTypes(TYPE_PROJECT_MEASURES)
      .setFetchSource(false)
      .setQuery(esQuery)
      .setFrom(searchOptions.getOffset())
      .setSize(searchOptions.getLimit())
      .addAggregation(createRangeFacet(DUPLICATED_LINES_DENSITY_KEY, ImmutableList.of(3d, 5d, 10d, 20d)))
      .addAggregation(createRangeFacet(COVERAGE_KEY, ImmutableList.of(30d, 50d, 70d, 80d)))
      .addAggregation(createRangeFacet(NCLOC_KEY, ImmutableList.of(1_000d, 10_000d, 100_000d, 500_000d)))
      .addAggregation(createRatingFacet(SQALE_RATING_KEY))
      .addAggregation(createRatingFacet(RELIABILITY_RATING_KEY))
      .addAggregation(createRatingFacet(SECURITY_RATING_KEY))
      .addSort(FIELD_NAME + "." + SORT_SUFFIX, SortOrder.ASC);

    return new SearchIdResult<>(request.get(), id -> id);
  }

  private static AggregationBuilder createRangeFacet(String metricKey, List<Double> thresholds) {
    RangeBuilder rangeAgg = AggregationBuilders.range(metricKey).field(FIELD_VALUE);
    final int lastIndex = thresholds.size() - 1;
    IntStream.range(0, thresholds.size())
      .forEach(i -> {
        if (i == 0) {
          rangeAgg.addUnboundedTo(thresholds.get(0));
          rangeAgg.addRange(thresholds.get(0), thresholds.get(1));
        } else if (i == lastIndex) {
          rangeAgg.addUnboundedFrom(thresholds.get(lastIndex));
        } else {
          rangeAgg.addRange(thresholds.get(i), thresholds.get(i + 1));
        }
      });

    return AggregationBuilders.nested("nested_" + metricKey)
      .path(FIELD_MEASURES)
      .subAggregation(
        AggregationBuilders.filter("filter_" + metricKey)
          .filter(termsQuery(FIELD_KEY, metricKey))
          .subAggregation(rangeAgg));
  }

  private static AggregationBuilder createRatingFacet(String metricKey) {
    return AggregationBuilders.nested("nested_" + metricKey)
      .path(FIELD_MEASURES)
      .subAggregation(
        AggregationBuilders.filter("filter_" + metricKey)
          .filter(termsQuery(FIELD_KEY, metricKey))
          .subAggregation(filters(metricKey)
            .filter("1", termQuery(FIELD_VALUE, 1d))
            .filter("2", termQuery(FIELD_VALUE, 2d))
            .filter("3", termQuery(FIELD_VALUE, 3d))
            .filter("4", termQuery(FIELD_VALUE, 4d))
            .filter("5", termQuery(FIELD_VALUE, 5d))
          ));
  }

  private QueryBuilder createEsQuery(ProjectMeasuresQuery query) {
    BoolQueryBuilder filters = boolQuery()
      .must(createAuthorizationFilter());
    query.getMetricCriteria().stream()
      .map(criterion -> nestedQuery(FIELD_MEASURES, boolQuery()
        .filter(termQuery(FIELD_KEY, criterion.getMetricKey()))
        .filter(toValueQuery(criterion))))
      .forEach(filters::filter);
    if (query.hasQualityGateStatus()) {
      filters.filter(termQuery(FIELD_QUALITY_GATE, query.getQualityGateStatus().name()));
    }
    return filters;
  }

  private static QueryBuilder toValueQuery(MetricCriterion criterion) {
    String fieldName = FIELD_VALUE;

    switch (criterion.getOperator()) {
      case GT:
        return rangeQuery(fieldName).gt(criterion.getValue());
      case LTE:
        return rangeQuery(fieldName).lte(criterion.getValue());
      case EQ:
        return termQuery(fieldName, criterion.getValue());
      default:
        throw new IllegalStateException("Metric criteria non supported: " + criterion.getOperator().name());
    }
  }

  private QueryBuilder createAuthorizationFilter() {
    Integer userLogin = userSession.getUserId();
    Set<String> userGroupNames = userSession.getUserGroups();
    BoolQueryBuilder groupsAndUser = boolQuery();
    if (userLogin != null) {
      groupsAndUser.should(termQuery(FIELD_AUTHORIZATION_USERS, userLogin.longValue()));
    }
    for (String group : userGroupNames) {
      groupsAndUser.should(termQuery(FIELD_AUTHORIZATION_GROUPS, group));
    }
    return QueryBuilders.hasParentQuery(TYPE_AUTHORIZATION,
      QueryBuilders.boolQuery().must(matchAllQuery()).filter(groupsAndUser));
  }
}
