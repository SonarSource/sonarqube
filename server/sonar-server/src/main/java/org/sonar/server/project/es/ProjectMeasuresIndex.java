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
package org.sonar.server.project.es;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;

import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndex extends BaseIndex {

  public ProjectMeasuresIndex(EsClient client) {
    super(client);
  }

  public SearchIdResult<String> search(SearchOptions searchOptions) {
    QueryBuilder condition = QueryBuilders.matchAllQuery();

    SearchRequestBuilder request = getClient()
      .prepareSearch(INDEX_PROJECT_MEASURES)
      .setTypes(TYPE_PROJECT_MEASURES)
      .setFetchSource(false)
      .setQuery(condition)
      .setFrom(searchOptions.getOffset())
      .setSize(searchOptions.getLimit())
      .addSort(FIELD_NAME + "." + SORT_SUFFIX, SortOrder.ASC);

    return new SearchIdResult<>(request.get(), id -> id);
  }
}
