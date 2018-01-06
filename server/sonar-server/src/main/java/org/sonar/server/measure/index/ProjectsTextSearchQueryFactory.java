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
package org.sonar.server.measure.index;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.sonar.server.es.DefaultIndexSettings;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NAME;

/**
 * This class is used in order to do some advanced full text search on projects key and name
 */
class ProjectsTextSearchQueryFactory {

  private ProjectsTextSearchQueryFactory() {
    // Only static methods
  }

  static QueryBuilder createQuery(String queryText) {
    BoolQueryBuilder featureQuery = boolQuery();
    Arrays.stream(ComponentTextSearchFeature.values())
      .map(f -> f.getQuery(queryText))
      .forEach(featureQuery::should);
    return featureQuery;
  }

  private enum ComponentTextSearchFeature {

    EXACT_IGNORE_CASE {
      @Override
      QueryBuilder getQuery(String queryText) {
        return matchQuery(SORTABLE_ANALYZER.subField(FIELD_NAME), queryText)
          .boost(2.5f);
      }
    },
    PREFIX {
      @Override
      QueryBuilder getQuery(String queryText) {
        return prefixAndPartialQuery(queryText, FIELD_NAME, FIELD_NAME)
          .boost(2f);
      }
    },
    PREFIX_IGNORE_CASE {
      @Override
      QueryBuilder getQuery(String queryText) {
        String lowerCaseQueryText = queryText.toLowerCase(Locale.ENGLISH);
        return prefixAndPartialQuery(lowerCaseQueryText, SORTABLE_ANALYZER.subField(FIELD_NAME), FIELD_NAME)
          .boost(3f);
      }
    },
    PARTIAL {
      @Override
      QueryBuilder getQuery(String queryText) {
        BoolQueryBuilder queryBuilder = boolQuery();
        split(queryText)
          .map(text -> partialTermQuery(text, FIELD_NAME))
          .forEach(queryBuilder::must);
        return queryBuilder
          .boost(0.5f);
      }
    },
    KEY {
      @Override
      QueryBuilder getQuery(String queryText) {
        return matchQuery(SORTABLE_ANALYZER.subField(FIELD_KEY), queryText)
          .boost(50f);
      }
    };

    abstract QueryBuilder getQuery(String queryText);

    protected Stream<String> split(String queryText) {
      return Arrays.stream(
        queryText.split(DefaultIndexSettings.SEARCH_TERM_TOKENIZER_PATTERN))
        .filter(StringUtils::isNotEmpty);
    }

    protected BoolQueryBuilder prefixAndPartialQuery(String queryText, String fieldName, String originalFieldName) {
      BoolQueryBuilder queryBuilder = boolQuery();
      AtomicBoolean first = new AtomicBoolean(true);
      split(queryText)
        .map(queryTerm -> {
          if (first.getAndSet(false)) {
            return prefixQuery(fieldName, queryTerm);
          }
          return partialTermQuery(queryTerm, originalFieldName);
        })
        .forEach(queryBuilder::must);
      return queryBuilder;
    }

    protected MatchQueryBuilder partialTermQuery(String queryTerm, String fieldName) {
      // We will truncate the search to the maximum length of nGrams in the index.
      // Otherwise the search would for sure not find any results.
      String truncatedQuery = StringUtils.left(queryTerm, DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH);
      return matchQuery(SEARCH_GRAMS_ANALYZER.subField(fieldName), truncatedQuery);
    }
  }
}
