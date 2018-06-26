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
package org.sonar.server.component.index;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filters.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filters.FiltersAggregator.KeyedFilter;
import org.elasticsearch.search.aggregations.bucket.filters.InternalFilters;
import org.elasticsearch.search.aggregations.bucket.filters.InternalFilters.InternalBucket;
import org.elasticsearch.search.aggregations.metrics.tophits.InternalTopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.textsearch.ComponentTextSearchFeature;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;
import org.sonar.server.permission.index.AuthorizationTypeSupport;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_LANGUAGE;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_ORGANIZATION_UUID;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_TYPE_COMPONENT;
import static org.sonar.server.component.index.ComponentIndexDefinition.NAME_ANALYZERS;
import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;

public class ComponentIndex {

  private static final String FILTERS_AGGREGATION_NAME = "filters";
  private static final String DOCS_AGGREGATION_NAME = "docs";

  private final EsClient client;
  private final AuthorizationTypeSupport authorizationTypeSupport;
  private final System2 system2;

  public ComponentIndex(EsClient client, AuthorizationTypeSupport authorizationTypeSupport, System2 system2) {
    this.client = client;
    this.authorizationTypeSupport = authorizationTypeSupport;
    this.system2 = system2;
  }

  public SearchIdResult<String> search(ComponentQuery query, SearchOptions searchOptions) {
    SearchRequestBuilder requestBuilder = client
      .prepareSearch(INDEX_TYPE_COMPONENT)
      .setFetchSource(false)
      .setFrom(searchOptions.getOffset())
      .setSize(searchOptions.getLimit());

    BoolQueryBuilder esQuery = boolQuery();
    esQuery.filter(authorizationTypeSupport.createQueryFilter());
    setNullable(query.getQuery(), q -> {
      ComponentTextSearchQuery componentTextSearchQuery = ComponentTextSearchQuery.builder()
        .setQueryText(q)
        .setFieldKey(FIELD_KEY)
        .setFieldName(FIELD_NAME)
        .build();
      esQuery.must(ComponentTextSearchQueryFactory.createQuery(componentTextSearchQuery, ComponentTextSearchFeatureRepertoire.values()));
    });
    setEmptiable(query.getQualifiers(), q -> esQuery.must(termsQuery(FIELD_QUALIFIER, q)));
    setNullable(query.getLanguage(), l -> esQuery.must(termQuery(FIELD_LANGUAGE, l)));
    setNullable(query.getOrganizationUuid(), o -> esQuery.must(termQuery(FIELD_ORGANIZATION_UUID, o)));
    requestBuilder.setQuery(esQuery);
    requestBuilder.addSort(SORTABLE_ANALYZER.subField(FIELD_NAME), SortOrder.ASC);

    return new SearchIdResult<>(requestBuilder.get(), id -> id, system2.getDefaultTimeZone());
  }

  public ComponentIndexResults searchSuggestions(SuggestionQuery query) {
    return searchSuggestions(query, ComponentTextSearchFeatureRepertoire.values());
  }

  @VisibleForTesting
  ComponentIndexResults searchSuggestions(SuggestionQuery query, ComponentTextSearchFeature... features) {
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

  private static FiltersAggregationBuilder createAggregation(SuggestionQuery query) {
    return AggregationBuilders.filters(
      FILTERS_AGGREGATION_NAME,
      query.getQualifiers().stream().map(q -> new KeyedFilter(q, termQuery(FIELD_QUALIFIER, q))).toArray(KeyedFilter[]::new))
      .subAggregation(createSubAggregation(query));
  }

  private static TopHitsAggregationBuilder createSubAggregation(SuggestionQuery query) {
    return AggregationBuilders.topHits(DOCS_AGGREGATION_NAME)
      .highlighter(new HighlightBuilder()
        .encoder("html")
        .preTags("<mark>")
        .postTags("</mark>")
        .field(createHighlighterField()))
      .from(query.getSkip())
      .size(query.getLimit())
      .sort(new ScoreSortBuilder())
      .sort(new FieldSortBuilder(ComponentIndexDefinition.FIELD_NAME))
      .fetchSource(false);
  }

  private QueryBuilder createQuery(SuggestionQuery query, ComponentTextSearchFeature... features) {
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

    return new ComponentHitsPerQualifier(bucket.getKey(), ComponentHit.fromSearchHits(hits), hitList.getTotalHits());
  }

  private static <T> void setNullable(@Nullable T parameter, Consumer<T> consumer) {
    if (parameter != null) {
      consumer.accept(parameter);
    }
  }

  private static <T> void setEmptiable(Collection<T> parameter, Consumer<Collection<T>> consumer) {
    if (!parameter.isEmpty()) {
      consumer.accept(parameter);
    }
  }
}
