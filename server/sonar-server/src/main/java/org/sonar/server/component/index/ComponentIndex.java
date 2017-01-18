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
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.sonar.core.util.stream.Collectors;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.DefaultIndexSettings;
import org.sonar.server.es.EsClient;
import org.sonar.server.user.UserSession;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_AUTHORIZATION_GROUPS;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_AUTHORIZATION_USERS;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_COMPONENTS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_AUTHORIZATION;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;
import static org.sonar.server.es.DefaultIndexSettingsElement.FUZZY_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;

public class ComponentIndex extends BaseIndex {

  private final UserSession userSession;

  public ComponentIndex(EsClient client, UserSession userSession) {
    super(client);
    this.userSession = userSession;
  }

  public List<String> search(ComponentIndexQuery query) {
    SearchRequestBuilder request = getClient()
      .prepareSearch(INDEX_COMPONENTS)
      .setTypes(TYPE_COMPONENT)
      .setFetchSource(false);

    query.getLimit().ifPresent(request::setSize);

    request.setQuery(createQuery(query));

    return Arrays.stream(request.get().getHits().hits())
      .map(SearchHit::getId)
      .collect(Collectors.toList());
  }

  private QueryBuilder createQuery(ComponentIndexQuery query) {
    BoolQueryBuilder esQuery = boolQuery();
    esQuery.filter(createAuthorizationFilter());

    query.getQualifier().ifPresent(q -> esQuery.filter(termQuery(FIELD_QUALIFIER, q)));

    String queryText = query.getQuery();

    // We will truncate the search to the maximum length of nGrams in the index.
    // Otherwise the search would for sure not find any results.
    String truncatedQuery = StringUtils.left(queryText, DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH);

    return esQuery.must(boolQuery()

      // partial name matches
      .should(matchQuery(SEARCH_GRAMS_ANALYZER.subField(FIELD_NAME), truncatedQuery))

      // fuzzy name matches
      .should(matchQuery(FUZZY_ANALYZER.subField(FIELD_NAME), queryText).fuzziness(Fuzziness.AUTO))

      // prefix matches
      .should(prefixQuery(FUZZY_ANALYZER.subField(FIELD_NAME), queryText))

      // exact match on the key
      .should(matchQuery(SORTABLE_ANALYZER.subField(FIELD_KEY), queryText).boost(5f)));
  }

  private QueryBuilder createAuthorizationFilter() {
    Integer userLogin = userSession.getUserId();
    Set<String> userGroupNames = userSession.getUserGroups();
    BoolQueryBuilder groupsAndUser = boolQuery();

    Optional.ofNullable(userLogin)
      .map(Integer::longValue)
      .ifPresent(userId -> groupsAndUser.should(termQuery(FIELD_AUTHORIZATION_USERS, userId)));

    userGroupNames.stream()
      .forEach(group -> groupsAndUser.should(termQuery(FIELD_AUTHORIZATION_GROUPS, group)));

    return QueryBuilders.hasParentQuery(TYPE_AUTHORIZATION,
      QueryBuilders.boolQuery().must(matchAllQuery()).filter(groupsAndUser));
  }
}
