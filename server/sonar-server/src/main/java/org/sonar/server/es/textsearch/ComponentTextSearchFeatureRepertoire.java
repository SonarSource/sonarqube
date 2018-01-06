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
package org.sonar.server.es.textsearch;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.es.DefaultIndexSettings;
import org.sonar.server.es.DefaultIndexSettingsElement;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_PREFIX_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_PREFIX_CASE_INSENSITIVE_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.textsearch.ComponentTextSearchFeature.UseCase.CHANGE_ORDER_OF_RESULTS;
import static org.sonar.server.es.textsearch.ComponentTextSearchFeature.UseCase.GENERATE_RESULTS;

public enum ComponentTextSearchFeatureRepertoire implements ComponentTextSearchFeature {

  EXACT_IGNORE_CASE(CHANGE_ORDER_OF_RESULTS) {
    @Override
    public QueryBuilder getQuery(ComponentTextSearchQuery query) {
      return matchQuery(SORTABLE_ANALYZER.subField(query.getFieldName()), query.getQueryText())
        .boost(2.5f);
    }
  },
  PREFIX(CHANGE_ORDER_OF_RESULTS) {
    @Override
    public Stream<QueryBuilder> getQueries(ComponentTextSearchQuery query) {
      List<String> tokens = query.getQueryTextTokens();
      if (tokens.isEmpty()) {
        return Stream.empty();
      }
      BoolQueryBuilder queryBuilder = prefixAndPartialQuery(tokens, query.getFieldName(), SEARCH_PREFIX_ANALYZER)
        .boost(3f);
      return Stream.of(queryBuilder);
    }
  },
  PREFIX_IGNORE_CASE(GENERATE_RESULTS) {
    @Override
    public Stream<QueryBuilder> getQueries(ComponentTextSearchQuery query) {
      List<String> tokens = query.getQueryTextTokens();
      if (tokens.isEmpty()) {
        return Stream.empty();
      }
      List<String> lowerCaseTokens = tokens.stream().map(t -> t.toLowerCase(Locale.ENGLISH)).collect(MoreCollectors.toList());
      BoolQueryBuilder queryBuilder = prefixAndPartialQuery(lowerCaseTokens, query.getFieldName(), SEARCH_PREFIX_CASE_INSENSITIVE_ANALYZER)
        .boost(2f);
      return Stream.of(queryBuilder);
    }
  },
  PARTIAL(GENERATE_RESULTS) {
    @Override
    public Stream<QueryBuilder> getQueries(ComponentTextSearchQuery query) {
      List<String> tokens = query.getQueryTextTokens();
      if (tokens.isEmpty()) {
        return Stream.empty();
      }
      BoolQueryBuilder queryBuilder = boolQuery().boost(0.5f);
      tokens.stream()
        .map(text -> tokenQuery(text, query.getFieldName(), SEARCH_GRAMS_ANALYZER))
        .forEach(queryBuilder::must);
      return Stream.of(queryBuilder);
    }
  },
  KEY(GENERATE_RESULTS) {
    @Override
    public QueryBuilder getQuery(ComponentTextSearchQuery query) {
      return matchQuery(SORTABLE_ANALYZER.subField(query.getFieldKey()), query.getQueryText())
        .boost(50f);
    }
  },
  RECENTLY_BROWSED(CHANGE_ORDER_OF_RESULTS) {
    @Override
    public Stream<QueryBuilder> getQueries(ComponentTextSearchQuery query) {
      Set<String> recentlyBrowsedKeys = query.getRecentlyBrowsedKeys();
      if (recentlyBrowsedKeys.isEmpty()) {
        return Stream.empty();
      }
      return Stream.of(termsQuery(query.getFieldKey(), recentlyBrowsedKeys).boost(100f));
    }
  },
  FAVORITE(CHANGE_ORDER_OF_RESULTS) {
    @Override
    public Stream<QueryBuilder> getQueries(ComponentTextSearchQuery query) {
      Set<String> favoriteKeys = query.getFavoriteKeys();
      if (favoriteKeys.isEmpty()) {
        return Stream.empty();
      }
      return Stream.of(termsQuery(query.getFieldKey(), favoriteKeys).boost(1000f));
    }
  };

  private final UseCase useCase;

  ComponentTextSearchFeatureRepertoire(UseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  public QueryBuilder getQuery(ComponentTextSearchQuery query) {
    throw new UnsupportedOperationException();
  }

  protected BoolQueryBuilder prefixAndPartialQuery(List<String> tokens, String originalFieldName, DefaultIndexSettingsElement analyzer) {
    BoolQueryBuilder queryBuilder = boolQuery();
    AtomicBoolean first = new AtomicBoolean(true);
    tokens.stream()
      .map(queryTerm -> {

        if (first.getAndSet(false)) {
          return tokenQuery(queryTerm, originalFieldName, analyzer);
        }

        return tokenQuery(queryTerm, originalFieldName, SEARCH_GRAMS_ANALYZER);
      })
      .forEach(queryBuilder::must);
    return queryBuilder;
  }

  protected MatchQueryBuilder tokenQuery(String queryTerm, String fieldName, DefaultIndexSettingsElement analyzer) {
    // We will truncate the search to the maximum length of nGrams in the index.
    // Otherwise the search would for sure not find any results.
    String truncatedQuery = StringUtils.left(queryTerm, DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH);
    return matchQuery(analyzer.subField(fieldName), truncatedQuery);
  }

  @Override
  public UseCase getUseCase() {
    return useCase;
  }
}
