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
package org.sonar.server.component.index;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.metrics.tophits.InternalTopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.user.UserSession;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_AUTHORIZATION_GROUPS;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_AUTHORIZATION_USERS;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_COMPONENTS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_AUTHORIZATION;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;

public class ComponentIndex extends BaseIndex {

  private final UserSession userSession;

  public ComponentIndex(EsClient client, UserSession userSession) {
    super(client);
    this.userSession = userSession;
  }

  public List<ComponentsPerQualifier> search(ComponentIndexQuery query) {
    return search(query, ComponentIndexSearchFeature.values());
  }

  @VisibleForTesting
  List<ComponentsPerQualifier> search(ComponentIndexQuery query, ComponentIndexSearchFeature... features) {
    if (query.getQualifiers().isEmpty()) {
      return Collections.emptyList();
    }

    SearchRequestBuilder request = getClient()
      .prepareSearch(INDEX_COMPONENTS)
      .setTypes(TYPE_COMPONENT)
      .setFetchSource(false)

      // the search hits are part of the aggregations
      .setSize(0)

      .setQuery(createQuery(query, features));

    query.getQualifiers().stream()
      .map(q -> createAggregation(query, q))
      .forEach(request::addAggregation);

    SearchResponse searchResponse = request.get();

    return query.getQualifiers().stream()
      .flatMap(q -> {

        InternalFilter agg = searchResponse.getAggregations().get(q);
        InternalTopHits docs = agg.getAggregations().get("docs");

        SearchHits hits = docs.getHits();

        long totalHits = hits.totalHits();
        if (totalHits < 1) {
          return Stream.empty();
        }

        List<String> componentUuids = Arrays.stream(hits.getHits()).map(SearchHit::getId)
          .collect(Collectors.toList());

        return Stream.of(new ComponentsPerQualifier(q, componentUuids, totalHits));
      })
      .collect(Collectors.toList());
  }

  /**
   * Create one aggregation per qualifier,
   *
   * @param query
   * @param qualifier
   * @return
   */
  private static FilterAggregationBuilder createAggregation(ComponentIndexQuery query, String qualifier) {
    return AggregationBuilders.filter(qualifier)
      .filter(termQuery(FIELD_QUALIFIER, qualifier))
      .subAggregation(createSubAggregation(query));
  }

  private static TopHitsBuilder createSubAggregation(ComponentIndexQuery query) {
    TopHitsBuilder sub = AggregationBuilders.topHits("docs");
    query.getLimit().ifPresent(sub::setSize);
    return sub.setFetchSource(false);
  }

  private QueryBuilder createQuery(ComponentIndexQuery query, ComponentIndexSearchFeature... features) {
    BoolQueryBuilder esQuery = boolQuery();
    esQuery.filter(createAuthorizationFilter());

    BoolQueryBuilder featureQuery = boolQuery();

    Arrays.stream(features)
      .map(f -> f.getQuery(query.getQuery()))
      .forEach(featureQuery::should);

    return esQuery.must(featureQuery);
  }

  private QueryBuilder createAuthorizationFilter() {
    Integer userLogin = userSession.getUserId();
    Set<String> userGroupNames = userSession.getUserGroups();
    BoolQueryBuilder groupsAndUser = boolQuery();

    Optional.ofNullable(userLogin)
      .map(Integer::longValue)
      .ifPresent(userId -> groupsAndUser.should(termQuery(FIELD_AUTHORIZATION_USERS, userId)));

    userGroupNames.stream()
      .forEach(group -> groupsAndUser.should(termQuery(FIELD_AUTHORIZATION_GROUPS, group)));

    return QueryBuilders.hasParentQuery(TYPE_AUTHORIZATION,
      QueryBuilders.boolQuery().must(matchAllQuery()).filter(groupsAndUser));
  }
}
