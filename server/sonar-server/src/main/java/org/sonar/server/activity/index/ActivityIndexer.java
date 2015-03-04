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
package org.sonar.server.activity.index;

import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import java.sql.Connection;
import java.util.Iterator;

/**
 * Add to Elasticsearch index {@link org.sonar.server.activity.index.ActivityIndexDefinition} the rows of
 * db table ACTIVITIES that are not indexed yet
 * <p/>
 */
public class ActivityIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public ActivityIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 0L, ActivityIndexDefinition.INDEX, ActivityIndexDefinition.TYPE);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    BulkIndexer bulk = new BulkIndexer(esClient, ActivityIndexDefinition.INDEX);
    bulk.setLarge(lastUpdatedAt == 0L);

    DbSession dbSession = dbClient.openSession(false);
    Connection dbConnection = dbSession.getConnection();
    try {
      ActivityResultSetIterator rowIt = ActivityResultSetIterator.create(dbClient, dbConnection, lastUpdatedAt);
      long maxUpdatedAt = doIndex(bulk, rowIt);
      rowIt.close();
      return maxUpdatedAt;

    } finally {
      dbSession.close();
    }
  }

  public long index(Iterator<ActivityDoc> activities) {
    BulkIndexer bulk = new BulkIndexer(esClient, ActivityIndexDefinition.INDEX);
    return doIndex(bulk, activities);
  }

  private long doIndex(BulkIndexer bulk, Iterator<ActivityDoc> activities) {
    long maxUpdatedAt = 0L;
    bulk.start();
    while (activities.hasNext()) {
      ActivityDoc activity = activities.next();
      bulk.add(newUpsertRequest(activity));

      // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
      maxUpdatedAt = Math.max(maxUpdatedAt, activity.getCreatedAt().getTime());
    }
    bulk.stop();
    return maxUpdatedAt;
  }

  private UpdateRequest newUpsertRequest(ActivityDoc doc) {
    return new UpdateRequest(ActivityIndexDefinition.INDEX, ActivityIndexDefinition.TYPE, doc.getKey())
      .doc(doc.getFields())
      .upsert(doc.getFields());
  }
}
