/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.source.index;

import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.api.ServerComponent;
import org.sonar.server.es.EsClient;

public class SourceLineIndex implements ServerComponent {

  private final EsClient esClient;

  public SourceLineIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  public void deleteLinesFromFileAbove(String fileUuid, int lastLine) {
    esClient.prepareDeleteByQuery(SourceLineIndexDefinition.INDEX_SOURCE_LINES)
      .setTypes(SourceLineIndexDefinition.TYPE_SOURCE_LINE)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(SourceLineIndexDefinition.FIELD_FILE_UUID, fileUuid))
        .must(QueryBuilders.rangeQuery(SourceLineIndexDefinition.FIELD_LINE).gt(lastLine))
      ).get();
  }
}
