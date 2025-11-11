/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.lucene.search.TotalHits;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.textsearch.ComponentTextSearchFeature;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;

import static java.util.Optional.ofNullable;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.index.ComponentIndexDefinition.NAME_ANALYZERS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;

public class ComponentIndex {

  private static final String FILTERS_AGGREGATION_NAME = "filters";
  private static final String DOCS_AGGREGATION_NAME = "docs";

  private final EsClient client;
  private final WebAuthorizationTypeSupport authorizationTypeSupport;
  private final System2 system2;

  public ComponentIndex(EsClient client, WebAuthorizationTypeSupport authorizationTypeSupport, System2 system2) {
    this.client = client;
    this.authorizationTypeSupport = authorizationTypeSupport;
    this.system2 = system2;
  }

  /**
   * Search using the new Elasticsearch Java API Client (8.x).
   */
  public SearchIdResult<String> searchV2(ComponentQuery query, SearchOptions searchOptions) {
    Query esQuery = createSearchQueryV2(query);

    SearchResponse<Void> response = client.searchV2(req -> req
        .index(TYPE_COMPONENT.getMainType().getIndex().getName())
        .query(esQuery)
        .source(s -> s.fetch(false))
        .trackTotalHits(t -> t.enabled(true))
        .from(searchOptions.getOffset())
        .size(searchOptions.getLimit())
        .sort(sort -> sort.field(f -> f
          .field(SORTABLE_ANALYZER.subField(FIELD_NAME))
          .order(SortOrder.Asc))),
      Void.class);

    return new SearchIdResult<>(response, id -> id, system2.getDefaultTimeZone().toZoneId());
  }

  private Query createSearchQueryV2(ComponentQuery query) {
    List<Query> filterQueries = new ArrayList<>();

    // Authorization filter
    filterQueries.add(authorizationTypeSupport.createQueryFilterV2());

    // Text search query (optional)
    List<Query> mustQueries = new ArrayList<>();
    if (query.getQuery() != null) {
      ComponentTextSearchQuery componentTextSearchQuery = ComponentTextSearchQuery.builder()
        .setQueryText(query.getQuery())
        .setFieldKey(FIELD_KEY)
        .setFieldName(FIELD_NAME)
        .build();
      mustQueries.add(ComponentTextSearchQueryFactory.createQueryV2(componentTextSearchQuery, ComponentTextSearchFeatureRepertoire.values()));
    }

    // Qualifiers filter (optional)
    if (!query.getQualifiers().isEmpty()) {
      List<FieldValue> qualifierValues = query.getQualifiers().stream()
        .map(FieldValue::of)
        .toList();
      mustQueries.add(Query.of(q -> q.terms(t -> t
        .field(FIELD_QUALIFIER)
        .terms(tf -> tf.value(qualifierValues)))));
    }

    return Query.of(q -> q.bool(b -> {
      b.filter(filterQueries);
      if (!mustQueries.isEmpty()) {
        b.must(mustQueries);
      }
      return b;
    }));
  }

  /**
   * Search suggestions using the new Elasticsearch Java API Client (8.x).
   */
  public ComponentIndexResults searchSuggestionsV2(SuggestionQuery query) {
    return searchSuggestionsV2(query, ComponentTextSearchFeatureRepertoire.values());
  }

  @VisibleForTesting
  ComponentIndexResults searchSuggestionsV2(SuggestionQuery query, ComponentTextSearchFeature... features) {
    Collection<String> qualifiers = query.getQualifiers();
    if (qualifiers.isEmpty()) {
      return ComponentIndexResults.newBuilder().build();
    }

    Query esQuery = createQueryV2(query, features);

    // Build keyed filters map
    Map<String, Query> filtersMap = qualifiers.stream()
      .collect(Collectors.toMap(
        qualifier -> qualifier,
        qualifier -> Query.of(q -> q.term(t -> t
          .field(FIELD_QUALIFIER)
          .value(qualifier)))));

    SearchResponse<Void> response = client.searchV2(req -> req
        .index(TYPE_COMPONENT.getMainType().getIndex().getName())
        .query(esQuery)
        // search hits are part of the aggregations
        .size(0)
        .aggregations(FILTERS_AGGREGATION_NAME, agg -> agg
          .filters(f -> f.filters(b -> b.keyed(filtersMap)))
          .aggregations(DOCS_AGGREGATION_NAME, subAgg -> subAgg
            .topHits(th -> th
              .from(query.getSkip())
              .size(query.getLimit())
              .sort(sort -> sort.score(s -> s.order(SortOrder.Desc)))
              .sort(sort -> sort.field(f -> f.field(FIELD_NAME).order(SortOrder.Asc)))
              .source(s -> s.fetch(false))
              .highlight(h -> h
                .encoder(co.elastic.clients.elasticsearch.core.search.HighlighterEncoder.Html)
                .preTags("<mark>")
                .postTags("</mark>")
                .fields(FIELD_NAME, hf -> hf
                  .type("fvh")
                  .matchedFields(
                    Stream.concat(
                        Stream.of(FIELD_NAME),
                        Arrays.stream(NAME_ANALYZERS).map(a -> a.subField(FIELD_NAME)))
                      .toList())))))),
      Void.class);

    return aggregationsToQualifiersV2(response);
  }

  /**
   * Create query using the new Elasticsearch Java API Client (8.x).
   */
  private Query createQueryV2(SuggestionQuery query, ComponentTextSearchFeature... features) {
    ComponentTextSearchQuery componentTextSearchQuery = ComponentTextSearchQuery.builder()
      .setQueryText(query.getQuery())
      .setFieldKey(FIELD_KEY)
      .setFieldName(FIELD_NAME)
      .setRecentlyBrowsedKeys(query.getRecentlyBrowsedKeys())
      .setFavoriteKeys(query.getFavoriteKeys())
      .build();

    Query textSearchQuery =
      ComponentTextSearchQueryFactory.createQueryV2(componentTextSearchQuery, features);

    Query authQuery = authorizationTypeSupport.createQueryFilterV2();

    Query indexTypeQuery =
      Query.of(q -> q.term(t -> t
        .field(FIELD_INDEX_TYPE)
        .value(TYPE_COMPONENT.getName())));

    return Query.of(q -> q.bool(b -> b
      .filter(indexTypeQuery)
      .filter(authQuery)
      .must(textSearchQuery)));
  }

  private static ComponentIndexResults aggregationsToQualifiersV2(SearchResponse<Void> response) {
    co.elastic.clients.elasticsearch._types.aggregations.FiltersAggregate filtersAgg =
      response.aggregations().get(FILTERS_AGGREGATION_NAME).filters();

    return ComponentIndexResults.newBuilder()
      .setQualifiers(
        filtersAgg.buckets().keyed().entrySet().stream()
          .map(entry -> bucketToQualifierV2(entry.getKey(), entry.getValue())))
      .build();
  }

  private static ComponentHitsPerQualifier bucketToQualifierV2(String key, FiltersBucket bucket) {
    co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate topHitsAgg =
      bucket.aggregations().get(DOCS_AGGREGATION_NAME).topHits();

    return new ComponentHitsPerQualifier(
      key,
      ComponentHit.fromSearchHitsV2(topHitsAgg.hits().hits()),
      getTotalHitsV2(topHitsAgg.hits().total()).value);
  }

  private static TotalHits getTotalHitsV2(@Nullable co.elastic.clients.elasticsearch.core.search.TotalHits totalHits) {
    return ofNullable(totalHits)
      .map(total -> {
        TotalHits.Relation relation = switch (total.relation()) {
          case Eq -> TotalHits.Relation.EQUAL_TO;
          case Gte -> TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO;
        };
        return new TotalHits(total.value(), relation);
      })
      .orElseThrow(() -> new IllegalStateException("Could not get total hits of search results"));
  }
}
