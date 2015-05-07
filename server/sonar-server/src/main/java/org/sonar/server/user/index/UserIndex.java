/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.user.index;

import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.ServerSide;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ServerSide
public class UserIndex {

  /**
   * Convert an Elasticsearch result (a map) to an {@link UserDoc}. It's
   * used for {@link org.sonar.server.es.SearchResult}.
   */
  private static final Function<Map<String, Object>, UserDoc> DOC_CONVERTER = new NonNullInputFunction<Map<String, Object>, UserDoc>() {
    @Override
    protected UserDoc doApply(Map<String, Object> input) {
      return new UserDoc(input);
    }
  };

  public UserIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  private final EsClient esClient;

  @CheckForNull
  public UserDoc getNullableByLogin(String login) {
    GetRequestBuilder request = esClient.prepareGet(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, login)
      .setFetchSource(true)
      .setRouting(login);
    GetResponse response = request.get();
    if (response.isExists()) {
      return DOC_CONVERTER.apply(response.getSource());
    }
    return null;
  }

  /**
   * Returns the user associated with the given SCM account. If multiple users have the same
   * SCM account, then result is null.
   */
  @CheckForNull
  public UserDoc getNullableByScmAccount(String scmAccount) {
    List<UserDoc> users = getAtMostThreeActiveUsersForScmAccount(scmAccount);
    if (users.size() == 1) {
      return users.get(0);
    }
    return null;
  }

  public UserDoc getByLogin(String login) {
    UserDoc userDoc = getNullableByLogin(login);
    if (userDoc == null) {
      throw new NotFoundException(String.format("User '%s' not found", login));
    }
    return userDoc;
  }

  /**
   * Returns the active users (at most 3) who are associated to the given SCM account. This method can be used
   * to detect user conflicts.
   */
  public List<UserDoc> getAtMostThreeActiveUsersForScmAccount(String scmAccount) {
    List<UserDoc> result = new ArrayList<>();
    if (!StringUtils.isEmpty(scmAccount)) {
      SearchRequestBuilder request = esClient.prepareSearch(UserIndexDefinition.INDEX)
        .setTypes(UserIndexDefinition.TYPE_USER)
        .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
          FilterBuilders.boolFilter()
            .must(FilterBuilders.termFilter(UserIndexDefinition.FIELD_ACTIVE, true))
            .should(FilterBuilders.termFilter(UserIndexDefinition.FIELD_LOGIN, scmAccount))
            .should(FilterBuilders.termFilter(UserIndexDefinition.FIELD_EMAIL, scmAccount))
            .should(FilterBuilders.termFilter(UserIndexDefinition.FIELD_SCM_ACCOUNTS, scmAccount))))
        .setSize(3);
      for (SearchHit hit : request.get().getHits().getHits()) {
        result.add(DOC_CONVERTER.apply(hit.sourceAsMap()));
      }
    }
    return result;
  }

  public Iterator<UserDoc> selectUsersForBatch(List<String> logins) {
    BoolFilterBuilder filter = FilterBuilders.boolFilter()
      .must(FilterBuilders.termsFilter(UserIndexDefinition.FIELD_LOGIN, logins));

    SearchRequestBuilder requestBuilder = esClient
      .prepareSearch(UserIndexDefinition.INDEX)
      .setTypes(UserIndexDefinition.TYPE_USER)
      .setSearchType(SearchType.SCAN)
      .addSort(SortBuilders.fieldSort(UserIndexDefinition.FIELD_LOGIN).order(SortOrder.ASC))
      .setScroll(TimeValue.timeValueMinutes(EsUtils.SCROLL_TIME_IN_MINUTES))
      .setSize(10000)
      .setFetchSource(
        new String[] {UserIndexDefinition.FIELD_LOGIN, UserIndexDefinition.FIELD_NAME},
        null)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filter));
    SearchResponse response = requestBuilder.get();

    return EsUtils.scroll(esClient, response.getScrollId(), DOC_CONVERTER);
  }

  public SearchResult<UserDoc> search(@Nullable String searchText, SearchOptions options) {
    SearchRequestBuilder request = esClient.prepareSearch(UserIndexDefinition.INDEX)
      .setTypes(UserIndexDefinition.TYPE_USER)
      .setSize(options.getLimit())
      .setFrom(options.getOffset())
      .addSort(UserIndexDefinition.FIELD_NAME, SortOrder.ASC);

    BoolFilterBuilder userFilter = FilterBuilders.boolFilter()
      .must(FilterBuilders.termFilter(UserIndexDefinition.FIELD_ACTIVE, true));

    QueryBuilder query = null;
    if (StringUtils.isEmpty(searchText)) {
      query = QueryBuilders.matchAllQuery();
    } else {
      query = QueryBuilders.multiMatchQuery(searchText,
        UserIndexDefinition.FIELD_LOGIN,
        UserIndexDefinition.FIELD_LOGIN + "." + UserIndexDefinition.SEARCH_SUB_SUFFIX,
        UserIndexDefinition.FIELD_NAME,
        UserIndexDefinition.FIELD_NAME + "." + UserIndexDefinition.SEARCH_SUB_SUFFIX)
        .operator(MatchQueryBuilder.Operator.AND);
    }

    request.setQuery(QueryBuilders.filteredQuery(query,
      userFilter));

    return new SearchResult<>(request.get(), DOC_CONVERTER);
  }
}
