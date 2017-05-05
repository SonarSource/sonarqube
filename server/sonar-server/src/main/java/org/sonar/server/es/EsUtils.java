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
package org.sonar.server.es;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.joda.time.format.ISODateTimeFormat;
import org.sonar.core.util.stream.MoreCollectors;

import static java.lang.String.format;

public class EsUtils {

  public static final int SCROLL_TIME_IN_MINUTES = 3;

  /**
   * See https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-regexp-query.html
   */
  private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[#@&~<>\"{}()\\[\\].+*?^$\\\\|]");

  private EsUtils() {
    // only static methods
  }

  public static <D extends BaseDoc> List<D> convertToDocs(SearchHits hits, Function<Map<String, Object>, D> converter) {
    List<D> docs = new ArrayList<>();
    for (SearchHit hit : hits.getHits()) {
      docs.add(converter.apply(hit.getSource()));
    }
    return docs;
  }

  public static LinkedHashMap<String, Long> termsToMap(Terms terms) {
    LinkedHashMap<String, Long> map = new LinkedHashMap<>();
    List<Terms.Bucket> buckets = terms.getBuckets();
    for (Terms.Bucket bucket : buckets) {
      map.put(bucket.getKeyAsString(), bucket.getDocCount());
    }
    return map;
  }

  public static List<String> termsKeys(Terms terms) {
    return terms.getBuckets().stream()
      .map(Terms.Bucket::getKeyAsString)
      .collect(MoreCollectors.toList(terms.getBuckets().size()));
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

  public static BulkResponse executeBulkRequest(BulkRequestBuilder builder, String errorMessage, Object... errorMessageArgs) {
    BulkResponse bulkResponse = builder.get();
    if (bulkResponse.hasFailures()) {
      // do not use Preconditions as the message is expensive to generate (see buildFailureMessage())
      throw new IllegalStateException(format(errorMessage, errorMessageArgs) + ": " + bulkResponse.buildFailureMessage());
    }
    return bulkResponse;
  }

  public static <D extends BaseDoc> Iterator<D> scroll(EsClient esClient, String scrollId, Function<Map<String, Object>, D> docConverter) {
    return new DocScrollIterator<>(esClient, scrollId, docConverter);
  }

  /**
   * Escapes regexp special characters so that text can be forwarded from end-user input
   * to Elasticsearch regexp query (for instance attributes "include" and "exclude" of
   * term aggregations.
   */
  public static String escapeSpecialRegexChars(String str) {
    return SPECIAL_REGEX_CHARS.matcher(str).replaceAll("\\\\$0");
  }

  private static class DocScrollIterator<D extends BaseDoc> implements Iterator<D> {

    private final EsClient esClient;
    private final String scrollId;
    private final Function<Map<String, Object>, D> docConverter;

    private final Queue<SearchHit> hits = new ArrayDeque<>();

    private DocScrollIterator(EsClient esClient, String scrollId, Function<Map<String, Object>, D> docConverter) {
      this.esClient = esClient;
      this.scrollId = scrollId;
      this.docConverter = docConverter;
    }

    @Override
    public boolean hasNext() {
      if (hits.isEmpty()) {
        SearchScrollRequestBuilder esRequest = esClient.prepareSearchScroll(scrollId)
          .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES));
        Collections.addAll(hits, esRequest.get().getHits().getHits());
      }
      return !hits.isEmpty();
    }

    @Override
    public D next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return docConverter.apply(hits.poll().getSource());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Cannot remove item when scrolling");
    }
  }

  public static <ID> Iterator<ID> scrollIds(EsClient esClient, String scrollId, Function<String, ID> idConverter) {
    return new IdScrollIterator<>(esClient, scrollId, idConverter);
  }

  private static class IdScrollIterator<ID> implements Iterator<ID> {

    private final EsClient esClient;
    private final String scrollId;
    private final Function<String, ID> idConverter;

    private final Queue<SearchHit> hits = new ArrayDeque<>();

    private IdScrollIterator(EsClient esClient, String scrollId, Function<String, ID> idConverter) {
      this.esClient = esClient;
      this.scrollId = scrollId;
      this.idConverter = idConverter;
    }

    @Override
    public boolean hasNext() {
      if (hits.isEmpty()) {
        SearchScrollRequestBuilder esRequest = esClient.prepareSearchScroll(scrollId)
          .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES));
        Collections.addAll(hits, esRequest.get().getHits().getHits());
      }
      return !hits.isEmpty();
    }

    @Override
    public ID next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return idConverter.apply(hits.poll().getId());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Cannot remove item when scrolling");
    }
  }
}
