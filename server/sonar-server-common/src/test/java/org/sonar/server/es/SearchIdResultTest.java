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
package org.sonar.server.es;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchIdResultTest {

  private final ZoneId timeZone = ZoneId.of("UTC");
  private final Function<String, String> identity = id -> id;

  @Test
  @SuppressWarnings("unchecked")
  public void constructor_extracts_ids_and_total_from_response() {
    Hit<Void> hit1 = hit("id-1");
    Hit<Void> hit2 = hit("id-2");
    List<Hit<Void>> hits = List.of(hit1, hit2);
    TotalHits totalHits = TotalHits.of(t -> t.value(2L).relation(co.elastic.clients.elasticsearch.core.search.TotalHitsRelation.Eq));
    SearchResponse<Void> response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
    when(response.hits().total()).thenReturn(totalHits);
    when(response.hits().hits()).thenReturn(hits);

    SearchIdResult<String> result = new SearchIdResult<>(response, identity, timeZone);

    assertThat(result.getUuids()).containsExactly("id-1", "id-2");
    assertThat(result.getTotal()).isEqualTo(2L);
    assertThat(result.getFacets()).isNotNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void constructor_applies_id_converter() {
    Hit<Void> hit = hit("42");
    List<Hit<Void>> hits = List.of(hit);
    TotalHits totalHits = TotalHits.of(t -> t.value(1L).relation(co.elastic.clients.elasticsearch.core.search.TotalHitsRelation.Eq));
    SearchResponse<Void> response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
    when(response.hits().total()).thenReturn(totalHits);
    when(response.hits().hits()).thenReturn(hits);

    SearchIdResult<Integer> result = new SearchIdResult<>(response, Integer::parseInt, timeZone);

    assertThat(result.getUuids()).containsExactly(42);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void constructor_handles_empty_results() {
    SearchResponse<Void> response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
    TotalHits totalHits = TotalHits.of(t -> t.value(0L).relation(co.elastic.clients.elasticsearch.core.search.TotalHitsRelation.Eq));
    when(response.hits().total()).thenReturn(totalHits);
    when(response.hits().hits()).thenReturn(List.of());

    SearchIdResult<String> result = new SearchIdResult<>(response, identity, timeZone);

    assertThat(result.getUuids()).isEmpty();
    assertThat(result.getTotal()).isZero();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void constructor_throws_when_total_hits_is_null() {
    SearchResponse<Void> response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
    when(response.hits().total()).thenReturn(null);
    when(response.hits().hits()).thenReturn(List.of());

    assertThatThrownBy(() -> new SearchIdResult<>(response, identity, timeZone))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Could not get total hits");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void toString_does_not_throw() {
    SearchResponse<Void> response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
    TotalHits totalHits = TotalHits.of(t -> t.value(0L).relation(co.elastic.clients.elasticsearch.core.search.TotalHitsRelation.Eq));
    when(response.hits().total()).thenReturn(totalHits);
    when(response.hits().hits()).thenReturn(List.of());

    SearchIdResult<String> result = new SearchIdResult<>(response, identity, timeZone);

    assertThat(result.toString()).isNotBlank();
  }

  @SuppressWarnings("unchecked")
  private static Hit<Void> hit(String id) {
    Hit<Void> hit = mock(Hit.class);
    when(hit.id()).thenReturn(id);
    return hit;
  }
}