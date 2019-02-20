/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.ProjectIndexer;

import static org.sonar.server.permission.index.FooIndexDefinition.TYPE_FOO;

public class FooIndexer implements ProjectIndexer, NeedAuthorizationIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(TYPE_FOO, p -> true);

  private final EsClient esClient;

  public FooIndexer(EsClient esClient) {
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
    FooDoc fooDoc = new FooDoc(projectUuid, name);
    esClient.prepareIndex(TYPE_FOO)
      .setId(fooDoc.getId())
      .setRouting(fooDoc.getRouting().orElse(null))
      .setSource(fooDoc.getFields())
      .get();
  }

  private static final class FooDoc extends BaseDoc {
    private final String projectUuid;
    private final String name;

    private FooDoc(String projectUuid, String name) {
      super(TYPE_FOO);
      this.projectUuid = projectUuid;
      this.name = name;
      setField(FooIndexDefinition.FIELD_PROJECT_UUID, projectUuid);
      setField(FooIndexDefinition.FIELD_NAME, name);
      setParent(AuthorizationDoc.idOf(projectUuid));
    }

    @Override
    public String getId() {
      return projectUuid + "_" + name;
    }

  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(TYPE_FOO);
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    throw new UnsupportedOperationException();
  }
}
