/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.component.index;

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.sonar.server.es.DefaultIndexSettings;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;

public enum ComponentIndexSearchFeature {

  PARTIAL {
    @Override
    public QueryBuilder getQuery(String queryText) {
      BoolQueryBuilder query = boolQuery();
      split(queryText)
        .map(queryTerm -> {

          // We will truncate the search to the maximum length of nGrams in the index.
          // Otherwise the search would for sure not find any results.
          String truncatedQuery = StringUtils.left(queryTerm, DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH);

          return matchQuery(SEARCH_GRAMS_ANALYZER.subField(FIELD_NAME), truncatedQuery);
        })
        .forEach(query::must);
      return query;
    }
  },
  KEY {
    @Override
    public QueryBuilder getQuery(String queryText) {
      return matchQuery(SORTABLE_ANALYZER.subField(FIELD_KEY), queryText).boost(5f);
    }
  };

  /** Pattern, that splits the user search input **/
  public static final String SEARCH_TERM_TOKENIZER_PATTERN = "[\\:\\.\\s]+";

  public abstract QueryBuilder getQuery(String queryText);

  protected Stream<String> split(String queryText) {
    return Arrays.stream(
      queryText.split(SEARCH_TERM_TOKENIZER_PATTERN))
      .filter(StringUtils::isNotEmpty);
  }
}
