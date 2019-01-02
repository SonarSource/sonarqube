/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.sonar.server.es.textsearch.ComponentTextSearchFeature.UseCase;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.sonar.server.es.textsearch.JavaTokenizer.split;

/**
 * This class is used in order to do some advanced full text search in an index on component key and component name
 *
 * The index must contains at least one field for the component key and one field for the component name
 */
public class ComponentTextSearchQueryFactory {

  private ComponentTextSearchQueryFactory() {
    // Only static methods
  }

  public static QueryBuilder createQuery(ComponentTextSearchQuery query, ComponentTextSearchFeature... features) {
    checkArgument(features.length > 0, "features cannot be empty");
    BoolQueryBuilder esQuery = boolQuery().must(
      createQuery(query, features, UseCase.GENERATE_RESULTS)
        .orElseThrow(() -> new IllegalStateException("No text search features found to generate search results. Features: " + Arrays.toString(features))));
    createQuery(query, features, UseCase.CHANGE_ORDER_OF_RESULTS)
      .ifPresent(esQuery::should);
    return esQuery;
  }

  private static Optional<QueryBuilder> createQuery(ComponentTextSearchQuery query, ComponentTextSearchFeature[] features, UseCase useCase) {
    BoolQueryBuilder generateResults = boolQuery();
    AtomicBoolean anyFeatures = new AtomicBoolean();
    Arrays.stream(features)
      .filter(f -> f.getUseCase() == useCase)
      .peek(f -> anyFeatures.set(true))
      .flatMap(f -> f.getQueries(query))
      .forEach(generateResults::should);
    if (anyFeatures.get()) {
      return Optional.of(generateResults);
    }
    return Optional.empty();
  }

  public static class ComponentTextSearchQuery {
    private final String queryText;
    private final List<String> queryTextTokens;
    private final String fieldKey;
    private final String fieldName;
    private final Set<String> recentlyBrowsedKeys;
    private final Set<String> favoriteKeys;

    private ComponentTextSearchQuery(Builder builder) {
      this.queryText = builder.queryText;
      this.queryTextTokens = split(builder.queryText);
      this.fieldKey = builder.fieldKey;
      this.fieldName = builder.fieldName;
      this.recentlyBrowsedKeys = builder.recentlyBrowsedKeys;
      this.favoriteKeys = builder.favoriteKeys;
    }

    public String getQueryText() {
      return queryText;
    }

    public List<String> getQueryTextTokens() {
      return queryTextTokens;
    }

    public String getFieldKey() {
      return fieldKey;
    }

    public String getFieldName() {
      return fieldName;
    }

    public Set<String> getRecentlyBrowsedKeys() {
      return recentlyBrowsedKeys;
    }

    public static Builder builder() {
      return new Builder();
    }

    public Set<String> getFavoriteKeys() {
      return favoriteKeys;
    }

    public static class Builder {
      private String queryText;
      private String fieldKey;
      private String fieldName;
      private Set<String> recentlyBrowsedKeys = Collections.emptySet();
      private Set<String> favoriteKeys = Collections.emptySet();

      /**
       * The text search query
       */
      public Builder setQueryText(String queryText) {
        this.queryText = queryText;
        return this;
      }

      /**
       * The index field that contains the component key
       */
      public Builder setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
        return this;
      }

      /**
       * The index field that contains the component name
       */
      public Builder setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
      }

      /**
       * Component keys of recently browsed items
       */
      public Builder setRecentlyBrowsedKeys(Set<String> recentlyBrowsedKeys) {
        this.recentlyBrowsedKeys = ImmutableSet.copyOf(recentlyBrowsedKeys);
        return this;
      }

      /**
       * Component keys of favorite items
       */
      public Builder setFavoriteKeys(Set<String> favoriteKeys) {
        this.favoriteKeys = ImmutableSet.copyOf(favoriteKeys);
        return this;
      }

      public ComponentTextSearchQuery build() {
        requireNonNull(queryText, "query text cannot be null");
        requireNonNull(fieldKey, "field key cannot be null");
        requireNonNull(fieldName, "field name cannot be null");
        requireNonNull(recentlyBrowsedKeys, "field recentlyBrowsedKeys cannot be null");
        requireNonNull(favoriteKeys, "field favoriteKeys cannot be null");
        return new ComponentTextSearchQuery(this);
      }
    }
  }
}
