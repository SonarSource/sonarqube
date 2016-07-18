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
package org.sonar.server.user.index;

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

@ServerSide
@ComputeEngineSide
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

  private final EsClient esClient;

  public UserIndex(EsClient esClient) {
    this.esClient = esClient;
  }

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
   * Returns the active users (at most 3) who are associated to the given SCM account. This method can be used
   * to detect user conflicts.
   */
  public List<UserDoc> getAtMostThreeActiveUsersForScmAccount(String scmAccount) {
    List<UserDoc> result = new ArrayList<>();
    if (!StringUtils.isEmpty(scmAccount)) {
      SearchRequestBuilder request = esClient.prepareSearch(UserIndexDefinition.INDEX)
        .setTypes(UserIndexDefinition.TYPE_USER)
        .setQuery(boolQuery().must(matchAllQuery()).filter(
          boolQuery()
            .must(termQuery(UserIndexDefinition.FIELD_ACTIVE, true))
            .should(termQuery(UserIndexDefinition.FIELD_LOGIN, scmAccount))
            .should(termQuery(UserIndexDefinition.FIELD_EMAIL, scmAccount))
            .should(termQuery(UserIndexDefinition.FIELD_SCM_ACCOUNTS, scmAccount))))
        .setSize(3);
      for (SearchHit hit : request.get().getHits().getHits()) {
        result.add(DOC_CONVERTER.apply(hit.sourceAsMap()));
      }
    }
    return result;
  }

  public Iterator<UserDoc> selectUsersForBatch(List<String> logins) {
    BoolQueryBuilder filter = boolQuery()
      .must(termsQuery(UserIndexDefinition.FIELD_LOGIN, logins));

    SearchRequestBuilder requestBuilder = esClient
      .prepareSearch(UserIndexDefinition.INDEX)
      .setTypes(UserIndexDefinition.TYPE_USER)
      .setSearchType(SearchType.SCAN)
      .addSort(SortBuilders.fieldSort(UserIndexDefinition.FIELD_LOGIN).order(SortOrder.ASC))
      .setScroll(TimeValue.timeValueMinutes(EsUtils.SCROLL_TIME_IN_MINUTES))
      .setSize(10_000)
      .setFetchSource(
        new String[] {UserIndexDefinition.FIELD_LOGIN, UserIndexDefinition.FIELD_NAME},
        null)
      .setQuery(QueryBuilders.filteredQuery(matchAllQuery(), filter));
    SearchResponse response = requestBuilder.get();

    return EsUtils.scroll(esClient, response.getScrollId(), DOC_CONVERTER);
  }

  public SearchResult<UserDoc> search(@Nullable String searchText, SearchOptions options) {
    SearchRequestBuilder request = esClient.prepareSearch(UserIndexDefinition.INDEX)
      .setTypes(UserIndexDefinition.TYPE_USER)
      .setSize(options.getLimit())
      .setFrom(options.getOffset())
      .addSort(UserIndexDefinition.FIELD_NAME, SortOrder.ASC);

    BoolQueryBuilder userQuery = boolQuery()
      .must(termQuery(UserIndexDefinition.FIELD_ACTIVE, true));

    QueryBuilder query;
    if (StringUtils.isEmpty(searchText)) {
      query = matchAllQuery();
    } else {
      query = QueryBuilders.multiMatchQuery(searchText,
        UserIndexDefinition.FIELD_LOGIN,
        UserIndexDefinition.FIELD_LOGIN + "." + UserIndexDefinition.SEARCH_SUB_SUFFIX,
        UserIndexDefinition.FIELD_NAME,
        UserIndexDefinition.FIELD_NAME + "." + UserIndexDefinition.SEARCH_SUB_SUFFIX)
        .operator(MatchQueryBuilder.Operator.AND);
    }

    request.setQuery(QueryBuilders.filteredQuery(query,
      userQuery));

    return new SearchResult<>(request.get(), DOC_CONVERTER);
  }

}
