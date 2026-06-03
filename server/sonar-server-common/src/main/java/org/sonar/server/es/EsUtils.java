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
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.joda.time.format.ISODateTimeFormat;

public class EsUtils {

  public static final int SEARCH_AFTER_PAGE_SIZE = 500;

  /**
   * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-regexp-query.html">Elasticsearch Regexp query documentation</a>
   */
  private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[#@&~<>\"{}()\\[\\].+*?^$\\\\|]");

  private EsUtils() {
    // only static methods
  }

  @CheckForNull
  public static Date parseDateTime(@Nullable String s) {
    if (s == null) {
      return null;
    }
    return ISODateTimeFormat.dateTime().parseDateTime(s).toDate();
  }

  @CheckForNull
  public static String formatDateTime(@Nullable Date date) {
    if (date != null) {
      return ISODateTimeFormat.dateTime().print(date.getTime());
    }
    return null;
  }

  /**
   * ES 8: Optimize search_after pagination by specifying document sorting.
   * This provides efficient pagination without the overhead of maintaining scroll contexts.
   * Note: _doc and _id sorting cannot be used with search_after:
   * <ul>
   * <li>_doc is only for scroll API</li>
   * <li>_id requires fielddata to be enabled (disabled by default in ES 8)</li>
   * </ul>
   * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html#search-after">Elasticsearch search_after documentation</a>
   */
  public static void optimizeSearchAfterRequest(SearchRequest.Builder esSearch, String sortField) {
    // Sort by sortField to ensure deterministic ordering for search_after pagination
    // This sortField has doc_values enabled and provides a unique sort key for reliable pagination
    esSearch.sort(s -> s.field(f -> f.field(sortField).order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)));
  }

  /**
   * Escapes regexp special characters so that text can be forwarded from end-user input
   * to Elasticsearch regexp query (for instance attributes "include" and "exclude" of
   * term aggregations.
   */
  public static String escapeSpecialRegexChars(String str) {
    return SPECIAL_REGEX_CHARS.matcher(str).replaceAll("\\\\$0");
  }

  /**
   * ES 8: Iterate through search results using search_after API
   * This is the recommended approach for deep pagination in ES 8.
   * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html#search-after">Elasticsearch search_after documentation</a>
   */
  public static <T, I> Iterator<I> searchAfterIds(EsClient esClient, SearchRequest initialRequest, Class<T> tClass, Function<String, I> idConverter) {
    return new SearchAfterIterator<>(esClient, initialRequest, tClass, idConverter);
  }

  private static class SearchAfterIterator<T, I> implements Iterator<I> {

    private final EsClient esClient;
    private final SearchRequest initialRequest;
    private final Class<T> tClass;
    private final Function<String, I> idConverter;

    private final Queue<Hit<T>> hits = new ArrayDeque<>();
    private List<FieldValue> lastSortValues = null;
    private boolean hasMore = true;
    private boolean isFirstRequest = true;

    private SearchAfterIterator(EsClient esClient, SearchRequest initialRequest, Class<T> tClass, Function<String, I> idConverter) {
      this.esClient = esClient;
      this.initialRequest = initialRequest;
      this.tClass = tClass;
      this.idConverter = idConverter;
      fetchNextBatch();
    }

    private void fetchNextBatch() {
      if (!hasMore) {
        return;
      }

      co.elastic.clients.elasticsearch.core.SearchResponse<T> response;

      if (isFirstRequest) {
        // For the first request, use the initial request but disable source fetching
        // since we're using Void.class and only need document IDs
        response = esClient.searchV2(b -> b
          .index(initialRequest.index())
          .query(initialRequest.query())
          .size(initialRequest.size())
          .sort(initialRequest.sort())
          .source(s -> s.fetch(false)),
          tClass);
        isFirstRequest = false;
      } else {
        // For subsequent requests, add search_after with the last sort values
        final List<String> indexNames = initialRequest.index();
        final Query query = initialRequest.query();
        final Integer size = initialRequest.size();
        final List<SortOptions> sortOptions = initialRequest.sort();

        response = esClient.searchV2(b -> {
          b.index(indexNames)
            .query(query)
            .size(size)
            .source(s -> s.fetch(false));

          // Apply the same sort configuration as the initial request
          if (sortOptions != null && !sortOptions.isEmpty()) {
            b.sort(sortOptions);
          }

          // Add search_after for pagination
          b.searchAfter(lastSortValues);

          return b;
        }, tClass);
      }

      if (response.hits() != null && response.hits().hits() != null && !response.hits().hits().isEmpty()) {
        List<Hit<T>> newHits = response.hits().hits();
        hits.addAll(newHits);
        // Save the sort values from the last hit for the next request
        Hit<T> lastHit = newHits.get(newHits.size() - 1);
        lastSortValues = lastHit.sort();
      } else {
        hasMore = false;
      }
    }

    @Override
    public boolean hasNext() {
      if (hits.isEmpty() && hasMore) {
        fetchNextBatch();
      }
      return !hits.isEmpty();
    }

    @Override
    public I next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return idConverter.apply(hits.poll().id());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Cannot remove item when iterating");
    }
  }
}
