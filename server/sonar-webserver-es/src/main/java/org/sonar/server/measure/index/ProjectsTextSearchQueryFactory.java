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
package org.sonar.server.measure.index;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.sonar.server.es.ES8QueryHelper;
import org.sonar.server.es.newindex.DefaultIndexSettings;

import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NAME;

/**
 * This class is used in order to do some advanced full text search on projects key and name
 */
class ProjectsTextSearchQueryFactory {

  private ProjectsTextSearchQueryFactory() {
    // Only static methods
  }

  static Query createQueryV2(String queryText) {
    return ES8QueryHelper.boolQuery(b -> Arrays.stream(ComponentTextSearchFeature.values())
      .map(f -> f.getQueryV2(queryText))
      .forEach(b::should));
  }

  private enum ComponentTextSearchFeature {

    EXACT_IGNORE_CASE {
      @Override
      Query getQueryV2(String queryText) {
        return Query.of(q -> q.match(m -> m
          .field(SORTABLE_ANALYZER.subField(FIELD_NAME))
          .query(queryText)
          .boost(2.5F)));
      }
    },
    PREFIX {
      @Override
      Query getQueryV2(String queryText) {
        return prefixAndPartialQueryV2(queryText, FIELD_NAME, FIELD_NAME, 2F);
      }
    },
    PREFIX_IGNORE_CASE {
      @Override
      Query getQueryV2(String queryText) {
        String lowerCaseQueryText = queryText.toLowerCase(Locale.ENGLISH);
        return prefixAndPartialQueryV2(lowerCaseQueryText, SORTABLE_ANALYZER.subField(FIELD_NAME), FIELD_NAME, 3F);
      }
    },
    PARTIAL {
      @Override
      Query getQueryV2(String queryText) {
        return Query.of(q -> q.bool(b -> {
          split(queryText)
            .map(text -> partialTermQueryV2(text, FIELD_NAME))
            .forEach(b::must);
          return b.boost(0.5F);
        }));
      }
    },
    KEY {
      @Override
      Query getQueryV2(String queryText) {
        return Query.of(q -> q.wildcard(w -> w
          .field(SORTABLE_ANALYZER.subField(FIELD_KEY))
          .value("*" + queryText + "*")
          .caseInsensitive(true)
          .boost(50F)));
      }
    };

    abstract Query getQueryV2(String queryText);

    protected Stream<String> split(String queryText) {
      return Arrays.stream(
        queryText.split(DefaultIndexSettings.SEARCH_TERM_TOKENIZER_PATTERN))
        .filter(StringUtils::isNotEmpty);
    }

    protected Query prefixAndPartialQueryV2(String queryText, String fieldName, String originalFieldName, float boost) {
      return Query.of(q -> q.bool(b -> {
        AtomicBoolean first = new AtomicBoolean(true);
        split(queryText)
          .map(queryTerm -> {
            if (first.getAndSet(false)) {
              return Query.of(qb -> qb.prefix(p -> p.field(fieldName).value(queryTerm)));
            }
            return partialTermQueryV2(queryTerm, originalFieldName);
          })
          .forEach(b::must);
        return b.boost(boost);
      }));
    }

    protected Query partialTermQueryV2(String queryTerm, String fieldName) {
      // We will truncate the search to the maximum length of nGrams in the index.
      // Otherwise the search would for sure not find any results.
      String truncatedQuery = StringUtils.left(queryTerm, DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH);
      return Query.of(q -> q.match(m -> m
        .field(SEARCH_GRAMS_ANALYZER.subField(fieldName))
        .query(truncatedQuery)));
    }
  }
}
