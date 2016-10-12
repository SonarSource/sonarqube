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

import java.util.Iterator;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public ProjectMeasuresIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 300, INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES, FIELD_ANALYSED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(createBulkIndexer(false), (String) null);
  }

  public void index(String projectUuid) {
    index(lastUpdatedAt -> doIndex(createBulkIndexer(false), projectUuid));
  }

  private long doIndex(BulkIndexer bulk, @Nullable String projectUuid) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ProjectMeasuresResultSetIterator rowIt = ProjectMeasuresResultSetIterator.create(dbClient, dbSession, projectUuid);
      doIndex(bulk, rowIt);
      rowIt.close();
      return 0L;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static void doIndex(BulkIndexer bulk, Iterator<ProjectMeasuresDoc> docs) {
    bulk.start();
    while (docs.hasNext()) {
      ProjectMeasuresDoc doc = docs.next();
      bulk.add(newIndexRequest(doc));
    }
    bulk.stop();
  }

  private BulkIndexer createBulkIndexer(boolean large) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_PROJECT_MEASURES);
    bulk.setLarge(large);
    return bulk;
  }

  private static IndexRequest newIndexRequest(ProjectMeasuresDoc doc) {
    return new IndexRequest(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES, doc.getId())
      .source(doc.getFields());
  }
}
