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
package org.sonar.server.es.textsearch;

import java.util.Arrays;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

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
    BoolQueryBuilder featureQuery = boolQuery();
    Arrays.stream(features)
      .map(f -> f.getQuery(query))
      .forEach(featureQuery::should);
    return featureQuery;
  }

  public static class ComponentTextSearchQuery {
    private final String queryText;
    private final String fieldKey;
    private final String fieldName;

    private ComponentTextSearchQuery(Builder builder) {
      this.queryText = builder.queryText;
      this.fieldKey = builder.fieldKey;
      this.fieldName = builder.fieldName;
    }

    public String getQueryText() {
      return queryText;
    }

    public String getFieldKey() {
      return fieldKey;
    }

    public String getFieldName() {
      return fieldName;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String queryText;
      private String fieldKey;
      private String fieldName;

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

      public ComponentTextSearchQuery build() {
        this.queryText = requireNonNull(queryText, "query text cannot be null");
        this.fieldKey = requireNonNull(fieldKey, "field key cannot be null");
        this.fieldName = requireNonNull(fieldName, "field name cannot be null");
        return new ComponentTextSearchQuery(this);
      }
    }
  }
}
