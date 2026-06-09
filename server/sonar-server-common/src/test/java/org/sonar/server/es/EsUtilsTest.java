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

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Test;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.es.EsUtils.escapeSpecialRegexChars;

public class EsUtilsTest {

  @Test
  public void util_class() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(EsUtils.class)).isTrue();
  }

  @Test
  public void es_date_format() {
    assertThat(EsUtils.formatDateTime(new Date(1_500_000_000_000L))).startsWith("2017-07-");
    assertThat(EsUtils.formatDateTime(null)).isNull();

    assertThat(EsUtils.parseDateTime("2017-07-14T04:40:00.000+02:00").getTime()).isEqualTo(1_500_000_000_000L);
    assertThat(EsUtils.parseDateTime(null)).isNull();
  }

  @Test
  public void test_escapeSpecialRegexChars() {
    assertThat(escapeSpecialRegexChars("")).isEmpty();
    assertThat(escapeSpecialRegexChars("foo")).isEqualTo("foo");
    assertThat(escapeSpecialRegexChars("FOO")).isEqualTo("FOO");
    assertThat(escapeSpecialRegexChars("foo++")).isEqualTo("foo\\+\\+");
    assertThat(escapeSpecialRegexChars("foo[]")).isEqualTo("foo\\[\\]");
    assertThat(escapeSpecialRegexChars(".*")).isEqualTo("\\.\\*");
    assertThat(escapeSpecialRegexChars("foo\\d")).isEqualTo("foo\\\\d");
    assertThat(escapeSpecialRegexChars("^")).isEqualTo("\\^");
    assertThat(escapeSpecialRegexChars("$")).isEqualTo("\\$");
    assertThat(escapeSpecialRegexChars("|")).isEqualTo("\\|");
    assertThat(escapeSpecialRegexChars("<")).isEqualTo("\\<");
    assertThat(escapeSpecialRegexChars(">")).isEqualTo("\\>");
    assertThat(escapeSpecialRegexChars("\"")).isEqualTo("\\\"");
    assertThat(escapeSpecialRegexChars("#")).isEqualTo("\\#");
    assertThat(escapeSpecialRegexChars("~")).isEqualTo("\\~");
    assertThat(escapeSpecialRegexChars("$")).isEqualTo("\\$");
    assertThat(escapeSpecialRegexChars("&")).isEqualTo("\\&");
    assertThat(escapeSpecialRegexChars("?")).isEqualTo("\\?");
    assertThat(escapeSpecialRegexChars("a bit of | & #<\"$ .* ^ everything")).isEqualTo("a bit of \\| \\& \\#\\<\\\"\\$ \\.\\* \\^ everything");
  }

  @Test
  public void optimizeSearchAfterRequest_adds_ascending_sort_on_given_field() {
    SearchRequest.Builder builder = new SearchRequest.Builder();
    EsUtils.optimizeSearchAfterRequest(builder, "uuid");
    SearchRequest request = builder.index("idx").build();

    assertThat(request.sort()).hasSize(1);
    SortOptions sort = request.sort().get(0);
    assertThat(sort.field().field()).isEqualTo("uuid");
    assertThat(sort.field().order()).isEqualTo(SortOrder.Asc);
  }

  // --- SearchAfterIterator ---

  @Test
  @SuppressWarnings("unchecked")
  public void searchAfterIds_returns_empty_iterator_when_no_results() {
    EsClient esClient = mock(EsClient.class);
    SearchResponse<Void> emptyResponse = emptyResponse();
    when(esClient.searchV2(any(), eq(Void.class))).thenReturn(emptyResponse);

    SearchRequest request = new SearchRequest.Builder().index("idx").build();
    Iterator<String> it = EsUtils.searchAfterIds(esClient, request, Void.class, id -> id);

    assertThat(it.hasNext()).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchAfterIds_next_on_empty_throws_NoSuchElementException() {
    EsClient esClient = mock(EsClient.class);
    SearchResponse<Void> empty = emptyResponse();
    when(esClient.searchV2(any(), eq(Void.class))).thenReturn(empty);

    SearchRequest request = new SearchRequest.Builder().index("idx").build();
    Iterator<String> it = EsUtils.searchAfterIds(esClient, request, Void.class, id -> id);

    assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchAfterIds_remove_always_throws() {
    EsClient esClient = mock(EsClient.class);
    SearchResponse<Void> empty = emptyResponse();
    when(esClient.searchV2(any(), eq(Void.class))).thenReturn(empty);

    SearchRequest request = new SearchRequest.Builder().index("idx").build();
    Iterator<String> it = EsUtils.searchAfterIds(esClient, request, Void.class, id -> id);

    assertThatThrownBy(it::remove).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchAfterIds_returns_all_ids_in_single_page() {
    EsClient esClient = mock(EsClient.class);
    SearchResponse<Void> page = pageResponse(List.of("id1", "id2", "id3"), List.of(FieldValue.of("id3")));
    SearchResponse<Void> empty = emptyResponse();
    when(esClient.searchV2(any(), eq(Void.class))).thenReturn(page, empty);

    SearchRequest request = new SearchRequest.Builder().index("idx").build();
    Iterator<String> it = EsUtils.searchAfterIds(esClient, request, Void.class, id -> id);

    assertThat(it.hasNext()).isTrue();
    assertThat(it.next()).isEqualTo("id1");
    assertThat(it.next()).isEqualTo("id2");
    assertThat(it.next()).isEqualTo("id3");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchAfterIds_paginates_across_multiple_pages() {
    EsClient esClient = mock(EsClient.class);
    SearchResponse<Void> page1 = pageResponse(List.of("a", "b"), List.of(FieldValue.of("b")));
    SearchResponse<Void> page2 = pageResponse(List.of("c"), List.of(FieldValue.of("c")));
    SearchResponse<Void> empty = emptyResponse();
    when(esClient.searchV2(any(), eq(Void.class))).thenReturn(page1, page2, empty);

    SearchRequest request = new SearchRequest.Builder().index("idx").build();
    Iterator<String> it = EsUtils.searchAfterIds(esClient, request, Void.class, id -> id);

    assertThat(it.next()).isEqualTo("a");
    assertThat(it.next()).isEqualTo("b");
    assertThat(it.next()).isEqualTo("c");
    assertThat(it.hasNext()).isFalse();
    // first request + second page + empty terminator
    verify(esClient, times(3)).searchV2(any(), eq(Void.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchAfterIds_applies_id_converter() {
    EsClient esClient = mock(EsClient.class);
    SearchResponse<Void> page = pageResponse(List.of("42"), List.of(FieldValue.of("42")));
    SearchResponse<Void> empty = emptyResponse();
    when(esClient.searchV2(any(), eq(Void.class))).thenReturn(page, empty);

    SearchRequest request = new SearchRequest.Builder().index("idx").build();
    Iterator<Integer> it = EsUtils.searchAfterIds(esClient, request, Void.class, Integer::parseInt);

    assertThat(it.next()).isEqualTo(42);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void searchAfterIds_multiple_hasNext_calls_do_not_advance_iterator() {
    EsClient esClient = mock(EsClient.class);
    SearchResponse<Void> page = pageResponse(List.of("x"), List.of(FieldValue.of("x")));
    SearchResponse<Void> empty = emptyResponse();
    when(esClient.searchV2(any(), eq(Void.class))).thenReturn(page, empty);

    SearchRequest request = new SearchRequest.Builder().index("idx").build();
    Iterator<String> it = EsUtils.searchAfterIds(esClient, request, Void.class, id -> id);

    assertThat(it.hasNext()).isTrue();
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next()).isEqualTo("x");
    assertThat(it.hasNext()).isFalse();
    assertThat(it.hasNext()).isFalse();
  }

  // --- helpers ---

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static SearchResponse<Void> emptyResponse() {
    SearchResponse<Void> response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);
    when(response.hits().hits()).thenReturn(List.of());
    return response;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static SearchResponse<Void> pageResponse(List<String> ids, List<FieldValue> sortValues) {
    SearchResponse<Void> response = mock(SearchResponse.class, RETURNS_DEEP_STUBS);

    List<Hit<Void>> hitList = ids.stream().map(id -> {
      Hit<Void> hit = mock(Hit.class);
      when(hit.id()).thenReturn(id);
      when(hit.sort()).thenReturn(sortValues);
      return hit;
    }).toList();

    when(response.hits().hits()).thenReturn(hitList);
    return response;
  }
}
