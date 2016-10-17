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

package org.sonar.server.permission.index;

import java.util.List;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.sonar.server.es.EsTester;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_AUTHORIZATION_USERS;

public class AuthorizationIndexerTester {

  private final EsTester esTester;

  private final AuthorizationIndexer authorizationIndexer;

  public AuthorizationIndexerTester(EsTester esTester) {
    this.esTester = esTester;
    this.authorizationIndexer = new AuthorizationIndexer(null, esTester.client());
  }

  public void insertProjectAuthorization(String projectUuid, List<String> groupNames, List<String> userLogins) {
    AuthorizationDao.Dto authorization = new AuthorizationDao.Dto(projectUuid, System.currentTimeMillis());
    groupNames.forEach(authorization::addUser);
    userLogins.forEach(authorization::addGroup);
    authorizationIndexer.index(singletonList(authorization));
  }

  public void verifyEmptyProjectAuthorization() {
    assertThat(esTester.countDocuments("issues", "authorization")).isZero();
  }

  public void verifyProjectAsNoAuthorization(String projectUuid) {
    verifyProjectAuthorization(projectUuid, emptyList(), emptyList());
  }

  public void verifyProjectAuthorization(String projectUuid, List<String> groupNames, List<String> userLogins) {
    assertThat(esTester.getIds("issues", "authorization")).containsOnly(projectUuid);
    BoolQueryBuilder queryBuilder = boolQuery().must(termQuery(FIELD_AUTHORIZATION_PROJECT_UUID, projectUuid));
    if (groupNames.isEmpty()) {
      queryBuilder.mustNot(existsQuery(FIELD_AUTHORIZATION_GROUPS));
    } else {
      queryBuilder.must(termsQuery(FIELD_AUTHORIZATION_GROUPS, groupNames));
    }
    if (userLogins.isEmpty()) {
      queryBuilder.mustNot(existsQuery(FIELD_AUTHORIZATION_USERS));
    } else {
      queryBuilder.must(termsQuery(FIELD_AUTHORIZATION_USERS, userLogins));
    }
    SearchRequestBuilder request = esTester.client()
      .prepareSearch("issues")
      .setTypes("authorization")
      .setQuery(boolQuery().must(matchAllQuery()).filter(queryBuilder));
    assertThat(request.get().getHits()).hasSize(1);
  }
}
