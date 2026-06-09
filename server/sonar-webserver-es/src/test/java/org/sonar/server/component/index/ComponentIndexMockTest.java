/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.JsonData;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentIndexMockTest {

  private final EsClient client = mock(EsClient.class);
  private final WebAuthorizationTypeSupport authorizationTypeSupport = mock(WebAuthorizationTypeSupport.class);
  private final System2 system2 = mock(System2.class);
  private final ComponentIndex underTest = new ComponentIndex(client, authorizationTypeSupport, system2);

  @Test
  public void searchSuggestionsV2_returns_empty_when_qualifiers_are_empty() {
    SuggestionQuery query = SuggestionQuery.builder()
      .setQuery("foo")
      .setQualifiers(List.of())
      .build();

    ComponentIndexResults results = underTest.searchSuggestionsV2(query);

    assertThat(results.getQualifiers().count()).isZero();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchSuggestionsV2_returns_qualifier_buckets_from_aggregations() {
    when(authorizationTypeSupport.createQueryFilterV2()).thenReturn(matchAllQuery());

    // Build a bucket with one hit and a total of 1
    Hit<JsonData> hit = mock(Hit.class);
    when(hit.id()).thenReturn("uuid-1");
    TopHitsAggregate topHits = mock(TopHitsAggregate.class, RETURNS_DEEP_STUBS);
    List<Hit<JsonData>> hitList = List.of(hit);
    when(topHits.hits().hits()).thenReturn(hitList);
    TotalHits total = TotalHits.of(t -> t.value(1L).relation(TotalHitsRelation.Eq));
    when(topHits.hits().total()).thenReturn(total);

    Aggregate docsAgg = mock(Aggregate.class);
    when(docsAgg.topHits()).thenReturn(topHits);

    FiltersBucket bucket = mock(FiltersBucket.class);
    when(bucket.aggregations()).thenReturn(Map.of("docs", docsAgg));

    FiltersAggregate filtersAgg = mock(FiltersAggregate.class, RETURNS_DEEP_STUBS);
    when(filtersAgg.buckets().keyed()).thenReturn(Map.of("TRK", bucket));

    Aggregate filtersAggregate = mock(Aggregate.class);
    when(filtersAggregate.filters()).thenReturn(filtersAgg);

    SearchResponse<Void> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(Map.of("filters", filtersAggregate));
    when(client.searchV2(any(), eq(Void.class))).thenReturn(response);

    SuggestionQuery query = SuggestionQuery.builder()
      .setQuery("foo")
      .setQualifiers(List.of("TRK"))
      .build();

    ComponentIndexResults results = underTest.searchSuggestionsV2(query);

    assertThat(results.getQualifiers().count()).isEqualTo(1L);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchSuggestionsV2_throws_when_total_hits_is_null() {
    when(authorizationTypeSupport.createQueryFilterV2()).thenReturn(matchAllQuery());

    TopHitsAggregate topHits = mock(TopHitsAggregate.class, RETURNS_DEEP_STUBS);
    when(topHits.hits().hits()).thenReturn(List.of());
    when(topHits.hits().total()).thenReturn(null);

    Aggregate docsAgg = mock(Aggregate.class);
    when(docsAgg.topHits()).thenReturn(topHits);

    FiltersBucket bucket = mock(FiltersBucket.class);
    when(bucket.aggregations()).thenReturn(Map.of("docs", docsAgg));

    FiltersAggregate filtersAgg = mock(FiltersAggregate.class, RETURNS_DEEP_STUBS);
    when(filtersAgg.buckets().keyed()).thenReturn(Map.of("TRK", bucket));

    Aggregate filtersAggregate = mock(Aggregate.class);
    when(filtersAggregate.filters()).thenReturn(filtersAgg);

    SearchResponse<Void> response = mock(SearchResponse.class);
    when(response.aggregations()).thenReturn(Map.of("filters", filtersAggregate));
    when(client.searchV2(any(), eq(Void.class))).thenReturn(response);

    SuggestionQuery query = SuggestionQuery.builder()
      .setQuery("foo")
      .setQualifiers(List.of("TRK"))
      .build();

    assertThatThrownBy(() -> underTest.searchSuggestionsV2(query))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Could not get total hits");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchV2_returns_empty_search_id_result_when_no_hits() {
    when(authorizationTypeSupport.createQueryFilterV2()).thenReturn(matchAllQuery());
    when(system2.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone(ZoneId.of("UTC")));

    SearchResponse<Void> response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
    TotalHits total = TotalHits.of(t -> t.value(0L).relation(TotalHitsRelation.Eq));
    when(response.hits().total()).thenReturn(total);
    when(response.hits().hits()).thenReturn(List.of());
    when(client.searchV2(any(), eq(Void.class))).thenReturn(response);

    ComponentQuery query = ComponentQuery.builder()
      .setQuery("name")
      .setQualifiers(List.of("TRK"))
      .build();

    SearchIdResult<String> result = underTest.searchV2(query, new SearchOptions());

    assertThat(result.getUuids()).isEmpty();
    assertThat(result.getTotal()).isZero();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchV2_passes_through_query_with_no_text_and_no_qualifiers() {
    when(authorizationTypeSupport.createQueryFilterV2()).thenReturn(matchAllQuery());
    when(system2.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone(ZoneId.of("UTC")));

    SearchResponse<Void> response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
    TotalHits total = TotalHits.of(t -> t.value(0L).relation(TotalHitsRelation.Eq));
    when(response.hits().total()).thenReturn(total);
    when(response.hits().hits()).thenReturn(List.of());
    when(client.searchV2(any(), eq(Void.class))).thenReturn(response);

    ComponentQuery query = ComponentQuery.builder()
      .setQuery(null)
      .setQualifiers(List.of())
      .build();

    SearchIdResult<String> result = underTest.searchV2(query, new SearchOptions());

    assertThat(result.getUuids()).isEmpty();
  }

  private static Query matchAllQuery() {
    return Query.of(q -> q.matchAll(m -> m));
  }
}