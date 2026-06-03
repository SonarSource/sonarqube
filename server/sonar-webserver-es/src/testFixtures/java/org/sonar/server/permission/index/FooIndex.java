/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.util.List;
import java.util.Map;
import org.sonar.server.es.EsClient;

import static org.sonar.server.permission.index.FooIndexDefinition.DESCRIPTOR;

public class FooIndex {

  private final EsClient esClient;
  private final WebAuthorizationTypeSupport authorizationTypeSupport;

  public FooIndex(EsClient esClient, WebAuthorizationTypeSupport authorizationTypeSupport) {
    this.esClient = esClient;
    this.authorizationTypeSupport = authorizationTypeSupport;
  }

  public boolean hasAccessToProject(String projectUuid) {
    Query authQuery = authorizationTypeSupport.createQueryFilterV2();
    SearchResponse<Map> response = esClient.searchV2(req -> req
      .index(DESCRIPTOR.getName())
      .query(q -> q.bool(b -> b
        .must(m -> m.term(t -> t.field(FooIndexDefinition.FIELD_PROJECT_UUID).value(projectUuid)))
        .filter(authQuery))),
      Map.class);
    List<String> names = response.hits().hits().stream()
      .map(h -> h.source().get(FooIndexDefinition.FIELD_NAME).toString())
      .toList();
    return names.size() == 2 && names.contains("bar") && names.contains("baz");
  }
}
