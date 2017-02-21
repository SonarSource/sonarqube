/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.permission.index;

import com.google.common.collect.ImmutableMap;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.ProjectIndexer;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.permission.index.FooIndexDefinition.FOO_INDEX;
import static org.sonar.server.permission.index.FooIndexDefinition.FOO_TYPE;
import static org.sonar.server.permission.index.FooIndexDefinition.INDEX_TYPE_FOO;

public class FooIndexer implements ProjectIndexer, NeedAuthorizationIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(INDEX_TYPE_FOO, p -> true);

  private final EsClient esClient;

  public FooIndexer(EsClient esClient) {
    this.esClient = esClient;
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public void indexProject(String projectUuid, Cause cause) {
    addToIndex(projectUuid, "bar");
    addToIndex(projectUuid, "baz");
  }

  private void addToIndex(String projectUuid, String name) {
    esClient.prepareIndex(INDEX_TYPE_FOO)
      .setRouting(projectUuid)
      .setParent(projectUuid)
      .setSource(ImmutableMap.of(
        FooIndexDefinition.FIELD_NAME, name,
        FooIndexDefinition.FIELD_PROJECT_UUID, projectUuid))
      .get();
  }

  @Override
  public void deleteProject(String projectUuid) {
    BulkIndexer.delete(esClient, FOO_INDEX, esClient.prepareSearch(FOO_INDEX)
      .setTypes(FOO_TYPE)
      .setQuery(boolQuery()
        .filter(
          termQuery(ComponentIndexDefinition.FIELD_PROJECT_UUID, projectUuid))));
  }
}
