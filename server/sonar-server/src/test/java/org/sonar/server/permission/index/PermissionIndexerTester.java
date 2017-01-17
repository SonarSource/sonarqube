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
import org.sonar.api.resources.Qualifiers;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class PermissionIndexerTester {

  private final EsTester esTester;

  private final PermissionIndexer permissionIndexer;

  public PermissionIndexerTester(EsTester esTester) {
    this.esTester = esTester;
    this.permissionIndexer = new PermissionIndexer(null, esTester.client());
  }

  public void indexProjectPermission(String projectUuid, List<String> groupNames, List<Long> userLogins) {
    PermissionIndexerDao.Dto authorization = new PermissionIndexerDao.Dto(projectUuid, System.currentTimeMillis(), Qualifiers.PROJECT);
    groupNames.forEach(authorization::addGroup);
    userLogins.forEach(authorization::addUser);
    permissionIndexer.index(authorization);
  }

  public void verifyProjectDoesNotExist(String projectUuid) {
    assertThat(esTester.getIds(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION)).doesNotContain(projectUuid);
    assertThat(esTester.getIds(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION)).doesNotContain(projectUuid);
    assertThat(esTester.getIds(ComponentIndexDefinition.INDEX_COMPONENTS, ComponentIndexDefinition.TYPE_AUTHORIZATION)).doesNotContain(projectUuid);
  }

  public void verifyProjectExistsWithoutPermission(String projectUuid) {
    verifyProjectExistsWithPermission(projectUuid, emptyList(), emptyList());
  }

  public void verifyProjectExistsWithPermission(String projectUuid, List<String> groupNames, List<Long> userLogins) {
    verifyComponentExistsWithPermissionInIndex(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION,
      IssueIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS, IssueIndexDefinition.FIELD_AUTHORIZATION_USERS,
      projectUuid, groupNames, userLogins);

    verifyComponentExistsWithPermissionInIndex(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION,
      ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_PROJECT_UUID, ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_GROUPS,
      ProjectMeasuresIndexDefinition.FIELD_AUTHORIZATION_USERS, projectUuid, groupNames, userLogins);

    verifyComponentExistsWithPermissionInIndex(ComponentIndexDefinition.INDEX_COMPONENTS, ComponentIndexDefinition.TYPE_AUTHORIZATION,
      "_id", ComponentIndexDefinition.FIELD_AUTHORIZATION_GROUPS,
      ComponentIndexDefinition.FIELD_AUTHORIZATION_USERS, projectUuid, groupNames, userLogins);
  }

  public void verifyViewExistsWithPermissionInRightIndexes(String viewUuid, List<String> groupNames, List<Long> userLogins) {
    // index issues
    verifyNoAuthorization(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION, viewUuid);

    // index project_measures
    verifyNoAuthorization(ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES, ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION, viewUuid);

    // index components
    verifyComponentExistsWithPermissionInIndex(ComponentIndexDefinition.INDEX_COMPONENTS, ComponentIndexDefinition.TYPE_AUTHORIZATION,
      "_id", ComponentIndexDefinition.FIELD_AUTHORIZATION_GROUPS,
      ComponentIndexDefinition.FIELD_AUTHORIZATION_USERS, viewUuid, groupNames, userLogins);
  }

  private void verifyComponentExistsWithPermissionInIndex(String index, String type, String projectField, String groupField,
    String userField, String componentUuid, List<String> groupNames, List<Long> userLogins) {
    assertThat(esTester.getIds(index, type)).contains(componentUuid);
    BoolQueryBuilder queryBuilder = boolQuery().must(termQuery(projectField, componentUuid));
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

  private void verifyNoAuthorization(String index, String authType, String authId) {
    assertThat(esTester.getIds(index, authType)).doesNotContain(authId);
  }

  private void verifyDocument(String index, String docType, String componentField, String componentUuid) {
    BoolQueryBuilder queryBuilder = boolQuery().must(termQuery(componentField, componentUuid));
    SearchRequestBuilder request = esTester.client()
        .prepareSearch(index)
        .setTypes(docType)
        .setQuery(boolQuery().must(matchAllQuery()).filter(queryBuilder));
    assertThat(request.get().getHits()).isNotEmpty();
  }
}
