/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.ProjectIndexer;

import static org.sonar.server.permission.index.FooIndexDefinition.INDEX_TYPE_FOO;

public class FooIndexer implements ProjectIndexer, NeedAuthorizationIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(INDEX_TYPE_FOO, p -> true);

  private final DbClient dbClient;
  private final EsClient esClient;

  public FooIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    addToIndex(branchUuid, "bar");
    addToIndex(branchUuid, "baz");
  }

  @Override
  public Collection<EsQueueDto> prepareForRecovery(DbSession dbSession, Collection<String> projectUuids, Cause cause) {
    throw new UnsupportedOperationException();
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
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_FOO);
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    throw new UnsupportedOperationException();
  }
}
