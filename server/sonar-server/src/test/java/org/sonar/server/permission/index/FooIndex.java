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

import java.util.Arrays;
import java.util.List;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.es.EsClient;

import static org.sonar.server.permission.index.FooIndexDefinition.FOO_INDEX;
import static org.sonar.server.permission.index.FooIndexDefinition.FOO_TYPE;

public class FooIndex {

  private final EsClient esClient;
  private final AuthorizationTypeSupport authorizationTypeSupport;

  public FooIndex(EsClient esClient, AuthorizationTypeSupport authorizationTypeSupport) {
    this.esClient = esClient;
    this.authorizationTypeSupport = authorizationTypeSupport;
  }

  public boolean hasAccessToProject(String projectUuid) {
    SearchHits hits = esClient.prepareSearch(FOO_INDEX)
      .setTypes(FOO_TYPE)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(FooIndexDefinition.FIELD_PROJECT_UUID, projectUuid))
        .filter(authorizationTypeSupport.createQueryFilter()))
      .get()
      .getHits();
    List<String> names = Arrays.stream(hits.hits())
      .map(h -> h.getSource().get(FooIndexDefinition.FIELD_NAME).toString())
      .collect(MoreCollectors.toList());
    return names.size() == 2 && names.contains("bar") && names.contains("baz");
  }
}
