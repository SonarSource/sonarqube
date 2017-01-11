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
import java.util.List;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.sonar.core.util.stream.Collectors;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_COMPONENTS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;

public class ComponentIndex extends BaseIndex {

  public ComponentIndex(EsClient client) {
    super(client);
  }

  public List<String> search(ComponentIndexQuery query) {
    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(INDEX_COMPONENTS)
      .setTypes(TYPE_COMPONENT)
      .setFetchSource(false);

    query.getLimit().ifPresent(requestBuilder::setSize);

    requestBuilder.setQuery(
      createQuery(query));

    return Arrays.stream(requestBuilder.get().getHits().hits())
      .map(SearchHit::getId)
      .collect(Collectors.toList());
  }

  private static QueryBuilder createQuery(ComponentIndexQuery query) {
    BoolQueryBuilder esQuery = boolQuery();

    query
      .getQualifiers()
      .forEach(qualifier -> esQuery.filter(termQuery(FIELD_QUALIFIER, qualifier)));

    return esQuery
      .must(
        boolQuery()
          .should(matchQuery(FIELD_NAME + "." + SEARCH_PARTIAL_SUFFIX, query.getQuery()))
          .should(termQuery(FIELD_KEY, query.getQuery()).boost(3f))
          .minimumNumberShouldMatch(1));
  }
}
