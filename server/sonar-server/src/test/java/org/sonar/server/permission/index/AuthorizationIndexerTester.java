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
import org.sonar.server.component.es.ProjectMeasuresIndexDefinition;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndexDefinition;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class AuthorizationIndexerTester {

  private final EsTester esTester;

  private final AuthorizationIndexer authorizationIndexer;

  public AuthorizationIndexerTester(EsTester esTester) {
    this.esTester = esTester;
    this.authorizationIndexer = new AuthorizationIndexer(null, esTester.client());
  }

  public void insertProjectAuthorization(String projectUuid, List<String> groupNames, List<String> userLogins) {
    AuthorizationDao.Dto authorization = new AuthorizationDao.Dto(projectUuid, System.currentTimeMillis());
    groupNames.forEach(authorization::addGroup);
    userLogins.forEach(authorization::addUser);
    authorizationIndexer.index(authorization);
  }

  public void verifyEmptyProjectAuthorization() {
    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION)).isZero();
    assertThat(esTester.countDocuments(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION)).isZero();
  }

  public void verifyProjectDoesNotExist(String projectUuid) {
    assertThat(esTester.getIds(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION)).doesNotContain(projectUuid);
    assertThat(esTester.getIds(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION)).doesNotContain(projectUuid);
  }

  public void verifyProjectExistsWithoutAuthorization(String projectUuid) {
    verifyProjectExistsWithAuthorization(projectUuid, emptyList(), emptyList());
  }

  public void verifyProjectExistsWithAuthorization(String projectUuid, List<String> groupNames, List<String> userLogins) {
    verifyProjectExistsWithAuthorizationInIndex(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION,
      IssueIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS, IssueIndexDefinition.FIELD_AUTHORIZATION_USERS,
      projectUuid, groupNames, userLogins);
    verifyProjectExistsWithAuthorizationInIndex(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION,
      ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_GROUPS,
      ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_USERS, projectUuid, groupNames, userLogins);
  }

  private void verifyProjectExistsWithAuthorizationInIndex(String index, String type, String projectField, String groupField, String userField, String projectUuid,
    List<String> groupNames, List<String> userLogins) {
    assertThat(esTester.getIds(index, type)).contains(projectUuid);
    BoolQueryBuilder queryBuilder = boolQuery().must(termQuery(projectField, projectUuid));
    if (groupNames.isEmpty()) {
      queryBuilder.mustNot(existsQuery(groupField));
    } else {
      queryBuilder.must(termsQuery(groupField, groupNames));
    }
    if (userLogins.isEmpty()) {
      queryBuilder.mustNot(existsQuery(userField));
    } else {
      queryBuilder.must(termsQuery(userField, userLogins));
    }
    SearchRequestBuilder request = esTester.client()
      .prepareSearch(index)
      .setTypes(type)
      .setQuery(boolQuery().must(matchAllQuery()).filter(queryBuilder));
    assertThat(request.get().getHits()).hasSize(1);
  }
}
