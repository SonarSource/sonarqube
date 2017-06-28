/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.component.index;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filters.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filters.InternalFilters;
import org.elasticsearch.search.aggregations.bucket.filters.InternalFilters.InternalBucket;
import org.elasticsearch.search.aggregations.metrics.tophits.InternalTopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.textsearch.ComponentTextSearchFeature;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;
import org.sonar.server.permission.index.AuthorizationTypeSupport;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_TYPE_COMPONENT;
import static org.sonar.server.component.index.ComponentIndexDefinition.NAME_ANALYZERS;

public class ComponentIndex {

  private static final String FILTERS_AGGREGATION_NAME = "filters";
  private static final String DOCS_AGGREGATION_NAME = "docs";

  private final EsClient client;
  private final AuthorizationTypeSupport authorizationTypeSupport;

  public ComponentIndex(EsClient client, AuthorizationTypeSupport authorizationTypeSupport) {
    this.client = client;
    this.authorizationTypeSupport = authorizationTypeSupport;
  }

  private static HighlightBuilder.Field createHighlighterField() {
    HighlightBuilder.Field field = new HighlightBuilder.Field(FIELD_NAME);
    field.highlighterType("fvh");
    field.matchedFields(
      Stream.concat(
        Stream.of(FIELD_NAME),
        Arrays
          .stream(NAME_ANALYZERS)
          .map(a -> a.subField(FIELD_NAME)))
        .toArray(String[]::new));
    return field;
  }

  public ComponentIndexResults search(ComponentIndexQuery query) {
    return search(query, ComponentTextSearchFeatureRepertoire.values());
  }

  @VisibleForTesting
  ComponentIndexResults search(ComponentIndexQuery query, ComponentTextSearchFeature... features) {
    Collection<String> qualifiers = query.getQualifiers();
    if (qualifiers.isEmpty()) {
      return ComponentIndexResults.newBuilder().build();
    }

    SearchRequestBuilder request = client
      .prepareSearch(INDEX_TYPE_COMPONENT)
      .setQuery(createQuery(query, features))
      .addAggregation(createAggregation(query))

      // the search hits are part of the aggregations
      .setSize(0);

    SearchResponse response = request.get();

    return aggregationsToQualifiers(response);
  }

  private static FiltersAggregationBuilder createAggregation(ComponentIndexQuery query) {
    return AggregationBuilders.filters(
      FILTERS_AGGREGATION_NAME,
      query.getQualifiers().stream().map(q -> termQuery(FIELD_QUALIFIER, q)).toArray(QueryBuilder[]::new))
      .subAggregation(createSubAggregation(query));
  }

  private static TopHitsAggregationBuilder createSubAggregation(ComponentIndexQuery query) {
    return AggregationBuilders.topHits(DOCS_AGGREGATION_NAME)
      .highlighter(new HighlightBuilder()
        .encoder("html")
        .preTags("<mark>")
        .postTags("</mark>")
        .field(createHighlighterField())
      )
      .from(query.getSkip())
      .size(query.getLimit())
      .sort(new ScoreSortBuilder())
      .sort(new FieldSortBuilder(ComponentIndexDefinition.FIELD_NAME))
      .fetchSource(false);
  }

  private QueryBuilder createQuery(ComponentIndexQuery query, ComponentTextSearchFeature... features) {
    BoolQueryBuilder esQuery = boolQuery();
    esQuery.filter(authorizationTypeSupport.createQueryFilter());
    ComponentTextSearchQuery componentTextSearchQuery = ComponentTextSearchQuery.builder()
      .setQueryText(query.getQuery())
      .setFieldKey(FIELD_KEY)
      .setFieldName(FIELD_NAME)
      .setRecentlyBrowsedKeys(query.getRecentlyBrowsedKeys())
      .setFavoriteKeys(query.getFavoriteKeys())
      .build();
    return esQuery.must(ComponentTextSearchQueryFactory.createQuery(componentTextSearchQuery, features));
  }

  private static ComponentIndexResults aggregationsToQualifiers(SearchResponse response) {
    InternalFilters filtersAgg = response.getAggregations().get(FILTERS_AGGREGATION_NAME);
    List<InternalBucket> buckets = filtersAgg.getBuckets();
    return ComponentIndexResults.newBuilder()
      .setQualifiers(
        buckets.stream().map(ComponentIndex::bucketToQualifier))
      .build();
  }

  private static ComponentHitsPerQualifier bucketToQualifier(InternalBucket bucket) {
    InternalTopHits docs = bucket.getAggregations().get(DOCS_AGGREGATION_NAME);

    SearchHits hitList = docs.getHits();
    SearchHit[] hits = hitList.getHits();

    return new ComponentHitsPerQualifier(bucket.getKey(), ComponentHit.fromSearchHits(hits), hitList.totalHits());
  }
}
