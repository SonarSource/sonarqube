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

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.ServerComponent;
import org.sonar.server.es.EsClient;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

public class UserIndex implements ServerComponent {

  private static final int SCROLL_TIME_IN_MINUTES = 3;

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
      return new UserDoc(response.getSource());
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
        result.add(new UserDoc(hit.sourceAsMap()));
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
      .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES))
      .setSize(10000)
      .setFetchSource(
        new String[] {UserIndexDefinition.FIELD_LOGIN, UserIndexDefinition.FIELD_NAME},
        null)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filter));
    SearchResponse response = requestBuilder.get();

    return scroll(response.getScrollId());
  }

  // Scrolling within the index
  private Iterator<UserDoc> scroll(final String scrollId) {
    return new Iterator<UserDoc>() {
      private final Queue<SearchHit> hits = new ArrayDeque<>();

      @Override
      public boolean hasNext() {
        if (hits.isEmpty()) {
          SearchScrollRequestBuilder esRequest = esClient.prepareSearchScroll(scrollId)
            .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES));
          Collections.addAll(hits, esRequest.get().getHits().getHits());
        }
        return !hits.isEmpty();
      }

      @Override
      public UserDoc next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return new UserDoc(hits.poll().getSource());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Cannot remove item when scrolling");
      }
    };
  }

}
