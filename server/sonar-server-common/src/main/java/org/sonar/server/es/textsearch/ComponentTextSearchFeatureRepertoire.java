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
package org.sonar.server.es.textsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.sonar.server.es.newindex.DefaultIndexSettings;
import org.sonar.server.es.newindex.DefaultIndexSettingsElement;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;

import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_PREFIX_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_PREFIX_CASE_INSENSITIVE_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.textsearch.ComponentTextSearchFeature.UseCase.CHANGE_ORDER_OF_RESULTS;
import static org.sonar.server.es.textsearch.ComponentTextSearchFeature.UseCase.GENERATE_RESULTS;

public enum ComponentTextSearchFeatureRepertoire implements ComponentTextSearchFeature {

  EXACT_IGNORE_CASE(CHANGE_ORDER_OF_RESULTS) {
    @Override
    public Query getQueryV2(ComponentTextSearchQuery query) {
      return Query.of(q -> q.match(m -> m
        .field(SORTABLE_ANALYZER.subField(query.getFieldName()))
        .query(query.getQueryText())
        .boost(2.5F)));
    }
  },
  PREFIX(CHANGE_ORDER_OF_RESULTS) {
    @Override
    public Stream<Query> getQueriesV2(ComponentTextSearchQuery query) {
      List<String> tokens = query.getQueryTextTokens();
      Query queryBuilder = prefixAndPartialQueryV2(tokens, query.getFieldName(), SEARCH_PREFIX_ANALYZER, 3F);
      return Stream.of(queryBuilder);
    }
  },
  PREFIX_IGNORE_CASE(GENERATE_RESULTS) {
    @Override
    public Stream<Query> getQueriesV2(ComponentTextSearchQuery query) {
      List<String> tokens = query.getQueryTextTokens();
      if (tokens.isEmpty()) {
        return Stream.empty();
      }
      List<String> lowerCaseTokens = tokens.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).toList();
      Query queryBuilder = prefixAndPartialQueryV2(lowerCaseTokens, query.getFieldName(), SEARCH_PREFIX_CASE_INSENSITIVE_ANALYZER, 2F);
      return Stream.of(queryBuilder);
    }
  },
  PARTIAL(GENERATE_RESULTS) {
    @Override
    public Stream<Query> getQueriesV2(ComponentTextSearchQuery query) {
      List<String> tokens = query.getQueryTextTokens();
      if (tokens.isEmpty()) {
        return Stream.empty();
      }
      List<Query> mustQueries = tokens.stream()
        .map(text -> tokenQueryV2(text, query.getFieldName(), SEARCH_GRAMS_ANALYZER))
        .toList();
      return Stream.of(Query.of(q -> q.bool(b -> b.must(mustQueries).boost(0.5F))));
    }
  },
  KEY(GENERATE_RESULTS) {
    @Override
    public Query getQueryV2(ComponentTextSearchQuery query) {
      return Query.of(q -> q.match(m -> m
        .field(SORTABLE_ANALYZER.subField(query.getFieldKey()))
        .query(query.getQueryText())
        .boost(50F)));
    }
  },
  RECENTLY_BROWSED(CHANGE_ORDER_OF_RESULTS) {
    @Override
    public Stream<Query> getQueriesV2(ComponentTextSearchQuery query) {
      Set<String> recentlyBrowsedKeys = query.getRecentlyBrowsedKeys();
      if (recentlyBrowsedKeys.isEmpty()) {
        return Stream.empty();
      }
      List<FieldValue> values = recentlyBrowsedKeys.stream()
        .map(FieldValue::of)
        .toList();
      return Stream.of(Query.of(q -> q.terms(t -> t
        .field(query.getFieldKey())
        .terms(tf -> tf.value(values))
        .boost(100F))));
    }
  },
  FAVORITE(CHANGE_ORDER_OF_RESULTS) {
    @Override
    public Stream<Query> getQueriesV2(ComponentTextSearchQuery query) {
      Set<String> favoriteKeys = query.getFavoriteKeys();
      if (favoriteKeys.isEmpty()) {
        return Stream.empty();
      }
      List<FieldValue> values = favoriteKeys.stream()
        .map(FieldValue::of)
        .toList();
      return Stream.of(Query.of(q -> q.terms(t -> t
        .field(query.getFieldKey())
        .terms(tf -> tf.value(values))
        .boost(1000F))));
    }
  };

  private final UseCase useCase;

  ComponentTextSearchFeatureRepertoire(UseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  public Query getQueryV2(ComponentTextSearchQuery query) {
    throw new UnsupportedOperationException("Use getQueriesV2 instead");
  }

  protected Query prefixAndPartialQueryV2(List<String> tokens, String originalFieldName, DefaultIndexSettingsElement analyzer, float boost) {
    List<Query> mustQueries = new ArrayList<>();
    AtomicBoolean first = new AtomicBoolean(true);
    for (String token : tokens) {
      var analyzerToUse = first.getAndSet(false) ? analyzer : SEARCH_GRAMS_ANALYZER;
      mustQueries.add(tokenQueryV2(token, originalFieldName, analyzerToUse));
    }
    return Query.of(q -> q.bool(b -> b.must(mustQueries).boost(boost)));
  }

  protected Query tokenQueryV2(String queryTerm, String fieldName, DefaultIndexSettingsElement analyzer) {
    // We will truncate the search to the maximum length of nGrams in the index.
    // Otherwise, the search would for sure not find any results.
    String truncatedQuery = StringUtils.left(queryTerm, DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH);
    return Query.of(q -> q.match(m -> m
      .field(analyzer.subField(fieldName))
      .query(truncatedQuery)));
  }

  @Override
  public UseCase getUseCase() {
    return useCase;
  }
}
