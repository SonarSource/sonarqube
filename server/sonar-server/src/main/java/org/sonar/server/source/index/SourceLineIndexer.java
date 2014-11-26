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

import com.google.common.annotations.VisibleForTesting;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import java.sql.Connection;
import java.util.Iterator;

public class SourceLineIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public SourceLineIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 0L, SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    final BulkIndexer bulk = new BulkIndexer(esClient, SourceLineIndexDefinition.INDEX);
    bulk.setLarge(lastUpdatedAt == 0L);

    DbSession dbSession = dbClient.openSession(false);
    Connection dbConnection = dbSession.getConnection();
    try {
      SourceLineResultSetIterator rowIt = SourceLineResultSetIterator.create(dbClient, dbConnection, lastUpdatedAt);
      long maxUpdatedAt = doIndex(bulk, rowIt);
      rowIt.close();
      return maxUpdatedAt;

    } finally {
      dbSession.close();
    }
  }

  @VisibleForTesting
  long index(Iterator<SourceLineResultSetIterator.SourceFile> sourceFiles) {
    final BulkIndexer bulk = new BulkIndexer(esClient, SourceLineIndexDefinition.INDEX);
    return doIndex(bulk, sourceFiles);
  }

  private long doIndex(BulkIndexer bulk, Iterator<SourceLineResultSetIterator.SourceFile> files) {
    long maxUpdatedAt = 0L;
    bulk.start();
    while (files.hasNext()) {
      SourceLineResultSetIterator.SourceFile file = files.next();
      for (SourceLineDoc line : file.getLines()) {
        bulk.add(newUpsertRequest(line));
      }
      deleteLinesFromFileAbove(file.getFileUuid(), file.getLines().size());
      maxUpdatedAt = Math.max(maxUpdatedAt, file.getUpdatedAt());
    }
    bulk.stop();
    return maxUpdatedAt;
  }

  private UpdateRequest newUpsertRequest(SourceLineDoc lineDoc) {
    String projectUuid = lineDoc.projectUuid();
    return new UpdateRequest(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE, lineDoc.key())
      .routing(projectUuid)
      .doc(lineDoc.getFields())
      .upsert(lineDoc.getFields());
  }

  /**
   * Unindex all lines in file with UUID <code>fileUuid</code> above line <code>lastLine</code>
   */
  private void deleteLinesFromFileAbove(String fileUuid, int lastLine) {
    esClient.prepareDeleteByQuery(SourceLineIndexDefinition.INDEX)
      .setTypes(SourceLineIndexDefinition.TYPE)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(SourceLineIndexDefinition.FIELD_FILE_UUID, fileUuid))
        .must(QueryBuilders.rangeQuery(SourceLineIndexDefinition.FIELD_LINE).gt(lastLine))
      ).get();
  }
}
