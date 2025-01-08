/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Optional;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.server.es.AnalysisIndexer;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.es.EsClient;

import static org.sonar.server.permission.index.FooIndexDefinition.TYPE_FOO;

public class FooIndexer implements AnalysisIndexer, NeedAuthorizationIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(TYPE_FOO, p -> true);

  private final EsClient esClient;
  private final DbClient dbClient;

  public FooIndexer(EsClient esClient, DbClient dbClient) {
    this.esClient = esClient;
    this.dbClient = dbClient;
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    try (DbSession dbSession = dbClient.openSession(true)) {
      Optional<BranchDto> branchDto = dbClient.branchDao().selectByUuid(dbSession, branchUuid);
      if (branchDto.isEmpty()) {
        //For portfolio, adding branchUuid directly
        addToIndex(branchUuid, "bar");
        addToIndex(branchUuid, "baz");
      } else {
        addToIndex(branchDto.get().getProjectUuid(), "bar");
        addToIndex(branchDto.get().getProjectUuid(), "baz");
      }
    }
  }

  private void addToIndex(String projectUuid, String name) {
    FooDoc fooDoc = new FooDoc(projectUuid, name);
    esClient.index(new IndexRequest(TYPE_FOO.getMainType().getIndex().getName())
      .type(TYPE_FOO.getMainType().getType())
      .id(fooDoc.getId())
      .routing(fooDoc.getRouting().orElse(null))
      .source(fooDoc.getFields()));
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
}
