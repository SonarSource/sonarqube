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

import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;

public class SourceLineIndexer implements ServerComponent {

  private final DbClient dbClient;
  private final EsClient esClient;
  private final SourceLineIndex index;
  private long lastUpdatedAt = 0L;

  public SourceLineIndexer(DbClient dbClient, EsClient esClient, SourceLineIndex index) {
    this.dbClient = dbClient;
    this.esClient = esClient;
    this.index = index;
  }

  public void indexSourceLines(boolean large) {
    final BulkIndexer bulk = new BulkIndexer(esClient, SourceLineIndexDefinition.INDEX_SOURCE_LINES);
    bulk.setLarge(large);

    DbSession dbSession = dbClient.openSession(false);
    Connection dbConnection = dbSession.getConnection();
    try {
      SourceLineResultSetIterator rowIt = SourceLineResultSetIterator.create(dbClient, dbConnection, getLastUpdatedAt());
      indexSourceLines(bulk, rowIt);
      rowIt.close();

    } finally {
      dbSession.close();
    }
  }

  public void indexSourceLines(BulkIndexer bulk, Iterator<Collection<SourceLineDoc>> sourceLines) {
    bulk.start();
    while (sourceLines.hasNext()) {
      Collection<SourceLineDoc> lineDocs = sourceLines.next();
      String fileUuid = null;
      int lastLine = 0;
      for (SourceLineDoc sourceLine: lineDocs) {
        lastLine ++;
        fileUuid = sourceLine.fileUuid();
        bulk.add(newUpsertRequest(sourceLine));
        long dtoUpdatedAt = sourceLine.updateDate().getTime();
        if (lastUpdatedAt < dtoUpdatedAt) {
          lastUpdatedAt = dtoUpdatedAt;
        }
      }
      index.deleteLinesFromFileAbove(fileUuid, lastLine);
    }
    bulk.stop();
  }

  private long getLastUpdatedAt() {
    long result;
    if (lastUpdatedAt <= 0L) {
      // request ES to get the max(updatedAt)
      result = esClient.getLastUpdatedAt(SourceLineIndexDefinition.INDEX_SOURCE_LINES, SourceLineIndexDefinition.TYPE_SOURCE_LINE);
    } else {
      // use cache. Will not work with Tomcat cluster.
      result = lastUpdatedAt;
    }
    return result;
  }

  private UpdateRequest newUpsertRequest(SourceLineDoc lineDoc) {
    String projectUuid = lineDoc.projectUuid();
    return new UpdateRequest(SourceLineIndexDefinition.INDEX_SOURCE_LINES, SourceLineIndexDefinition.TYPE_SOURCE_LINE, lineDoc.key())
      .routing(projectUuid)
      .doc(lineDoc.getFields())
      .upsert(lineDoc.getFields());
  }
}
